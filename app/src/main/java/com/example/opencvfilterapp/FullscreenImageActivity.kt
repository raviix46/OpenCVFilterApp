package com.example.opencvfilterapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.VelocityTracker
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.opencvfilterapp.databinding.ActivityFullscreenImageBinding
import java.io.InputStream
import kotlin.math.abs

class FullscreenImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullscreenImageBinding
    private lateinit var btnShare: ImageButton
    private lateinit var btnDelete: ImageButton
    private lateinit var btnFavorite: ImageButton
    private lateinit var photoInfo: TextView
    private lateinit var viewPager: ViewPager2

    private lateinit var imageUris: List<Uri>
    private var currentIndex = 0
    private val prefs by lazy { getSharedPreferences("gallery_prefs", MODE_PRIVATE) }
    private val favorites = mutableSetOf<String>()

    // Gesture constants
    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }
    private val dismissDistancePx = 250f
    private val dismissVelocityY = 1500f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullscreenImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // ✅ Create a parent container for drag animation
        binding.root.id = View.generateViewId()

        // Bind views
        viewPager = binding.fullscreenViewPager
        btnShare = binding.btnShare
        btnDelete = binding.btnDelete
        btnFavorite = binding.btnFavorite
        photoInfo = binding.photoInfo

        // Favorites
        favorites.addAll(prefs.getStringSet("favorites", emptySet()) ?: emptySet())

        // Intent data
        val urisStringList = intent.getStringArrayListExtra("allImageUris") ?: arrayListOf()
        imageUris = urisStringList.map { Uri.parse(it) }
        currentIndex = intent.getIntExtra("startPosition", 0).coerceIn(0, (imageUris.size - 1).coerceAtLeast(0))

        // Setup pager
        viewPager.adapter = FullscreenPagerAdapter(imageUris)
        if (imageUris.isNotEmpty()) {
            viewPager.setCurrentItem(currentIndex, false)
            updatePhotoInfo(imageUris[currentIndex])
            updateFavoriteIcon(imageUris[currentIndex])
        } else {
            Toast.makeText(this, "No images to display", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Change info when swiping between images
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentIndex = position
                val currentUri = imageUris[position]
                updatePhotoInfo(currentUri)
                updateFavoriteIcon(currentUri)
            }
        })

        // Buttons
        btnShare.setOnClickListener { shareImage(imageUris[currentIndex]) }
        btnDelete.setOnClickListener { deleteImage(imageUris[currentIndex]) }
        btnFavorite.setOnClickListener { toggleFavorite(imageUris[currentIndex]) }

        binding.galleryControls.alpha = 1f
    }

    // 🧠 Display EXIF info
    private fun updatePhotoInfo(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val exif = inputStream?.let { ExifInterface(it) }

            val filterInfo = exif?.getAttribute(ExifInterface.TAG_USER_COMMENT) ?: "Filter: Unknown"
            val timestamp = exif?.getAttribute(ExifInterface.TAG_DATETIME) ?: "Unknown Date"

            photoInfo.text = "🖋 $filterInfo • $timestamp"
        } catch (e: Exception) {
            e.printStackTrace()
            photoInfo.text = "❌ Metadata not found"
        }
    }

    // 📤 Share Image
    private fun shareImage(uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Image via"))
    }

    // 🗑 Delete Image + Notify GalleryActivity
    private fun deleteImage(uri: Uri) {
        try {
            val rowsDeleted = contentResolver.delete(uri, null, null)

            if (rowsDeleted > 0) {
                Toast.makeText(this, "🗑️ Image deleted", Toast.LENGTH_SHORT).show()

                val mutable = imageUris.toMutableList()
                val removedIndex = mutable.indexOf(uri)
                if (removedIndex != -1) mutable.removeAt(removedIndex)
                imageUris = mutable

                // ✅ Return result to GalleryActivity for instant refresh
                val resultIntent = Intent().apply {
                    putExtra("deletedUri", uri.toString())
                }
                setResult(RESULT_OK, resultIntent)

                // 🌀 Update UI or close if empty
                if (imageUris.isEmpty()) {
                    finishAfterTransition()
                } else {
                    viewPager.adapter = FullscreenPagerAdapter(imageUris)
                    currentIndex = currentIndex.coerceAtMost(imageUris.size - 1)
                    viewPager.setCurrentItem(currentIndex, false)
                    updatePhotoInfo(imageUris[currentIndex])
                    updateFavoriteIcon(imageUris[currentIndex])
                }
            } else {
                Toast.makeText(this, "⚠️ Failed to delete (not found)", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    // ❤️ Favorite toggle
    private fun toggleFavorite(uri: Uri) {
        val key = uri.toString()
        if (favorites.contains(key)) {
            favorites.remove(key)
            btnFavorite.setImageResource(R.drawable.ic_favorite_border)
            Toast.makeText(this, "💔 Removed from favorites", Toast.LENGTH_SHORT).show()
        } else {
            favorites.add(key)
            btnFavorite.setImageResource(R.drawable.ic_favorite)
            Toast.makeText(this, "❤️ Added to favorites", Toast.LENGTH_SHORT).show()
        }
        prefs.edit().putStringSet("favorites", favorites).apply()
        animateHeart()
    }

    private fun updateFavoriteIcon(uri: Uri) {
        btnFavorite.setImageResource(
            if (favorites.contains(uri.toString())) R.drawable.ic_favorite
            else R.drawable.ic_favorite_border
        )
    }

    private fun animateHeart() {
        btnFavorite.animate()
            .scaleX(1.3f)
            .scaleY(1.3f)
            .setDuration(120)
            .withEndAction {
                btnFavorite.animate().scaleX(1f).scaleY(1f).setDuration(120)
            }.start()
    }

    // --------------------------------
    // ViewPager Adapter with swipe-down-to-exit
    // --------------------------------
    private inner class FullscreenPagerAdapter(private val images: List<Uri>) :
        RecyclerView.Adapter<FullscreenPagerAdapter.ViewHolder>() {

        inner class ViewHolder(val imageView: android.widget.ImageView) :
            RecyclerView.ViewHolder(imageView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val iv = android.widget.ImageView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            }
            return ViewHolder(iv)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val uri = images[position]

            Glide.with(holder.imageView.context)
                .load(uri)
                .fitCenter()
                .into(holder.imageView)

            // tap toggles controls
            holder.imageView.setOnClickListener {
                val controls = binding.galleryControls
                controls.animate()
                    .alpha(if (controls.alpha == 1f) 0f else 1f)
                    .setDuration(200)
                    .start()
            }

            // 👇 enable swipe-down-to-exit
            holder.imageView.setOnTouchListener(makeSwipeToDismissTouchListener())
        }

        override fun getItemCount(): Int = images.size
    }

    // Gesture listener for swipe-down-to-exit
    private fun makeSwipeToDismissTouchListener(): View.OnTouchListener {
        var startX = 0f
        var startY = 0f
        var dragging = false
        var totalDy = 0f
        var tracker: VelocityTracker? = null

        return View.OnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = ev.x
                    startY = ev.y
                    totalDy = 0f
                    dragging = false
                    tracker = VelocityTracker.obtain().also { it.addMovement(ev) }
                    false
                }

                MotionEvent.ACTION_MOVE -> {
                    tracker?.addMovement(ev)
                    val dx = ev.x - startX
                    val dy = ev.y - startY

                    if (!dragging && dy > touchSlop && abs(dy) > abs(dx) + touchSlop) {
                        dragging = true
                        viewPager.requestDisallowInterceptTouchEvent(true)
                    }

                    if (dragging) {
                        totalDy = dy.coerceAtLeast(0f)
                        binding.root.translationY = totalDy / 2f
                        binding.root.alpha = 1 - (totalDy / 800f).coerceIn(0f, 0.8f)
                        true
                    } else false
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (dragging) {
                        tracker?.addMovement(ev)
                        tracker?.computeCurrentVelocity(1000)
                        val vy = tracker?.yVelocity ?: 0f
                        tracker?.recycle()
                        tracker = null

                        val shouldDismiss = totalDy > dismissDistancePx || vy > dismissVelocityY
                        if (shouldDismiss) {
                            finishAfterTransition()
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        } else {
                            binding.root.animate()
                                .translationY(0f)
                                .alpha(1f)
                                .setDuration(200)
                                .start()
                        }
                        true
                    } else false
                }

                else -> false
            }
        }
    }
}