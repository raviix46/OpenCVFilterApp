package com.example.opencvfilterapp

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.os.*
import android.provider.MediaStore
import android.view.*
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
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
    private enum class FilterMode { NONE, CARTOON, EDGE, BLUR, GRAY}
    @Volatile private var filterMode: FilterMode = FilterMode.GRAY

    private var filterIntensity: Int = 50
    private var outputBitmap: Bitmap? = null
    @Volatile private var latestFrame: Bitmap? = null

    private var frameCount = 0
    private var lastFpsTs = SystemClock.elapsedRealtime()

    companion object {
        init {
            System.loadLibrary("opencv_java4")
            System.loadLibrary("opencvfilterapp")
        }
    }

    // JNI bridge
    external fun processFrameJNI(input: Bitmap, output: Bitmap, mode: Int, intensity: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cameraView.surfaceTextureListener = this

        // 🎨 Define filters (matches native-lib.cpp order)
        val filters = listOf("None", "Cartoon", "Edge", "Blur", "Grayscale")

        // 🟣 Create a hint adapter (only shows "Select Filter" at startup)
        val hintAdapter = object : ArrayAdapter<String>(
            this,
            R.layout.spinner_item,
            listOf("🎨 Select Filter")
        ) {
            override fun isEnabled(position: Int): Boolean = false
        }

        // 🟢 Real adapter for filters
        val mainAdapter = ArrayAdapter(this, R.layout.spinner_item, filters).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

        // Show hint instantly
        binding.filterSpinner.adapter = hintAdapter

        // Replace with real adapter after short delay (no flicker)
        binding.filterSpinner.postDelayed({
            binding.filterSpinner.adapter = mainAdapter
            binding.filterSpinner.setSelection(-1, false)
        }, 300)

        // 🪄 Handle user selections
        binding.filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position == AdapterView.INVALID_POSITION) return

                val selected = filters[position]
                (view as? TextView)?.apply {
                    text = selected
                    setTextColor(Color.WHITE)
                    setTypeface(null, Typeface.NORMAL)
                }

                // 🔹 Apply filter logic without hiding thumbnail
                when (position) {
                    0 -> { // None
                        filterMode = FilterMode.NONE
                        binding.intensityPanel.visibility = View.GONE
                    }
                    1 -> { // Cartoon
                        filterMode = FilterMode.CARTOON
                        binding.intensityPanel.visibility = View.GONE
                    }
                    2 -> { // Edge
                        filterMode = FilterMode.EDGE
                        binding.intensityPanel.visibility = View.VISIBLE
                    }
                    3 -> { // Blur
                        filterMode = FilterMode.BLUR
                        binding.intensityPanel.visibility = View.VISIBLE
                    }
                    4 -> { // Grayscale
                        filterMode = FilterMode.GRAY
                        binding.intensityPanel.visibility = View.VISIBLE
                    }
                }

                // ✅ Keep thumbnail always visible (no flicker)
                if (binding.thumbnailPreview.drawable != null) {
                    binding.thumbnailPreview.visibility = View.VISIBLE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // 🟢 Default state
        filterMode = FilterMode.NONE
        binding.intensityPanel.visibility = View.GONE

        // 📸 Capture image
        binding.btnCapture.setOnClickListener {
            saveCurrentFrame()
            showFlashEffect()
            vibrateBriefly()
            //Toast.makeText(this, "📸 Image Saved Successfully!", Toast.LENGTH_SHORT).show()
        }

        // 🖼️ Open gallery
        binding.btnGallery.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
        }

        // 🎚️ Intensity slider
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

    // ---------- CAMERA ----------
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

        frameCount++
        val now = SystemClock.elapsedRealtime()
        if (now - lastFpsTs >= 1000) {
            binding.fpsText.text = "FPS: $frameCount"
            frameCount = 0
            lastFpsTs = now
        }
    }

    // ---------- ANIMATION ----------
    private fun moveThumbnail(up: Boolean) {
        if (binding.thumbnailPreview.visibility != View.VISIBLE) return
        val offsetY = -8f // tiny lift for style
        binding.thumbnailPreview.animate()
            .translationY(offsetY)
            .setDuration(250)
            .start()
    }

    // ---------- SAVE IMAGE ----------
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

            // ✅ Animated thumbnail update
            binding.thumbnailPreview.apply {
                if (visibility == View.VISIBLE) {
                    animate()
                        .translationY(-40f)
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction {
                            setImageBitmap(bmp)
                            translationY = 40f
                            alpha = 0f
                            visibility = View.VISIBLE
                            animate()
                                .translationY(0f)
                                .alpha(1f)
                                .setDuration(250)
                                .setInterpolator(android.view.animation.DecelerateInterpolator())
                                .start()
                        }
                        .start()
                } else {
                    setImageBitmap(bmp)
                    translationY = 40f
                    alpha = 0f
                    visibility = View.VISIBLE
                    animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(250)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .start()
                }
            }

            // ✅ Tap thumbnail to open image
            binding.thumbnailPreview.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            }

            // ✅ Stylish custom toast feedback
            // ✅ Stylish custom toast feedback with animation
            val toastView = layoutInflater.inflate(R.layout.custom_toast, null)
            toastView.findViewById<TextView>(R.id.toastText).text = "📸 Image Saved Successfully!"

            // 🎬 Smooth slide-down + fade-in animation
            toastView.translationY = -80f
            toastView.alpha = 0f
            toastView.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(300)
                .start()

            val toast = Toast(this)
            toast.view = toastView
            toast.duration = Toast.LENGTH_SHORT
            toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 180)
            toast.show()

        } catch (e: Exception) {
            e.printStackTrace()

            // 🔴 Themed error toast
            val toastView = layoutInflater.inflate(R.layout.custom_toast, null)
            val toastText = toastView.findViewById<TextView>(R.id.toastText)
            toastText.text = "❌ Save failed: ${e.message}"
            toastText.setTextColor(Color.WHITE)
            toastView.setBackgroundResource(android.R.color.holo_red_dark)

            val toast = Toast(this)
            toast.view = toastView
            toast.duration = Toast.LENGTH_SHORT
            toast.setGravity(Gravity.CENTER, 0, -250)
            toast.show()
        }
    }

    // ---------- EFFECTS ----------
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