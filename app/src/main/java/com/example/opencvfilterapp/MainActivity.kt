package com.example.opencvfilterapp

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.os.*
import android.provider.MediaStore
import android.view.*
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.opencvfilterapp.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import android.net.Uri
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator

class MainActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraCaptureSessions: CameraCaptureSession

    // ---------- Filter mode ----------
    private enum class FilterMode { NONE, GRAY, EDGE }
    @Volatile private var filterMode: FilterMode = FilterMode.GRAY

    // Intensity (default = 50%)
    private var filterIntensity: Int = 50

    // Reusable bitmap for processed frames
    private var outputBitmap: Bitmap? = null
    @Volatile private var latestFrame: Bitmap? = null

    // FPS counter
    private var frameCount = 0
    private var lastFpsTs = SystemClock.elapsedRealtime()

    companion object {
        init {
            System.loadLibrary("opencv_java4")
            System.loadLibrary("opencvfilterapp")
        }
    }

    // JNI: input bitmap, output bitmap, filter mode (0=none,1=gray,2=edge), intensity (0–100)
    external fun processFrameJNI(input: Bitmap, output: Bitmap, mode: Int, intensity: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cameraView.surfaceTextureListener = this

        // ---------- Buttons ----------
        binding.btnNone.setOnClickListener {
            filterMode = FilterMode.NONE
            binding.intensityPanel.visibility = View.GONE
            moveThumbnail(false)
        }

        binding.btnGray.setOnClickListener {
            filterMode = FilterMode.GRAY
            binding.intensityPanel.visibility = View.VISIBLE
            moveThumbnail(true)
        }

        binding.btnEdge.setOnClickListener {
            filterMode = FilterMode.EDGE
            binding.intensityPanel.visibility = View.VISIBLE
            moveThumbnail(true)
        }

        // ✅ Capture Image Button
        binding.btnCapture.setOnClickListener {
            saveCurrentFrame()
            showFlashEffect()
            vibrateBriefly()
            Toast.makeText(this, "📸 Image Saved Successfully!", Toast.LENGTH_SHORT).show()
        }

        // ✅ Intensity Slider
        binding.intensitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                filterIntensity = progress
                binding.intensityLabel.text = "Filter Intensity: $progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        ensureLegacyWritePermission()
    }

    // ---------- Camera ----------
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        openCamera()
    }

    private fun openCamera() {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        val cameraId = manager.cameraIdList[0]

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
            return
        }

        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                startCameraPreview()
            }
            override fun onDisconnected(camera: CameraDevice) { camera.close() }
            override fun onError(camera: CameraDevice, error: Int) { camera.close() }
        }, null)
    }

    private fun startCameraPreview() {
        val texture = binding.cameraView.surfaceTexture!!
        texture.setDefaultBufferSize(1920, 1080)
        val surface = Surface(texture)

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)

        cameraDevice.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    cameraCaptureSessions = session
                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                    cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                }
                override fun onConfigureFailed(session: CameraCaptureSession) {}
            },
            null
        )
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        val src = binding.cameraView.bitmap ?: return

        if (outputBitmap == null ||
            outputBitmap!!.width != src.width ||
            outputBitmap!!.height != src.height) {
            outputBitmap?.recycle()
            outputBitmap = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        }

        try {
            processFrameJNI(src, outputBitmap!!, filterMode.ordinal, filterIntensity)
            binding.processedPreview.setImageBitmap(outputBitmap)
            latestFrame = outputBitmap
        } catch (_: Throwable) {
            binding.processedPreview.setImageBitmap(src)
            latestFrame = src
        }

        // FPS counter
        frameCount++
        val now = SystemClock.elapsedRealtime()
        if (now - lastFpsTs >= 1000) {
            binding.fpsText.text = "FPS: $frameCount"
            frameCount = 0
            lastFpsTs = now
        }
    }

    // ---------- Thumbnail Animation ----------
    private fun moveThumbnail(up: Boolean) {
        // Move slightly higher (previously -80f → now -120f for better spacing)
        val targetY = if (up) -160f else 0f

        binding.thumbnailPreview.animate()
            .translationY(targetY)
            .setDuration(350)
            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
            .start()
    }

    // ---------- Capture & Save ----------
    private fun saveCurrentFrame() {
        val bmp = latestFrame ?: return toast("No frame available yet")
        val copy = bmp.copy(Bitmap.Config.ARGB_8888, false)
        saveBitmapToGallery(copy)
    }

    private fun saveBitmapToGallery(bmp: Bitmap) {
        val filename = "OpenCV_${System.currentTimeMillis()}.jpg"
        try {
            val uri: Uri = if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/OpenCVFilterApp")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val newUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw RuntimeException("MediaStore insert failed")

                contentResolver.openOutputStream(newUri)?.use {
                    bmp.compress(Bitmap.CompressFormat.JPEG, 95, it)
                }

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(newUri, values, null, null)
                newUri
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "OpenCVFilterApp")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, filename)
                FileOutputStream(file).use {
                    bmp.compress(Bitmap.CompressFormat.JPEG, 95, it)
                }
                Uri.fromFile(file)
            }

            // ✅ Update thumbnail preview
            binding.thumbnailPreview.setImageBitmap(bmp)
            binding.thumbnailPreview.visibility = View.VISIBLE

            // ✅ Open full image on click
            binding.thumbnailPreview.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            }

            toast("📸 Image Saved Successfully!")
        } catch (e: Exception) {
            e.printStackTrace()
            toast("Save failed: ${e.message}")
        }
    }

    // ---------- Flash & Vibration ----------
    private fun showFlashEffect() {
        val flashView = View(this)
        flashView.setBackgroundColor(Color.WHITE)
        flashView.alpha = 0f
        addContentView(flashView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        flashView.animate()
            .alpha(0.8f)
            .setDuration(100)
            .withEndAction {
                flashView.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction { (flashView.parent as? ViewGroup)?.removeView(flashView) }
            }
    }

    private fun vibrateBriefly() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(60)
        }
    }

    private fun ensureLegacyWritePermission() {
        if (Build.VERSION.SDK_INT <= 28 &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 42)
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}