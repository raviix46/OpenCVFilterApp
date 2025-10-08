package com.example.opencvfilterapp

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import androidx.viewpager2.widget.ViewPager2
import com.example.opencvfilterapp.databinding.ActivityGalleryBinding
import java.io.InputStream

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private val imageUris = mutableListOf<Uri>()
    private lateinit var adapter: GalleryAdapter

    private lateinit var viewPager: ViewPager2
    private lateinit var photoInfo: TextView
    private lateinit var btnShare: ImageButton
    private lateinit var btnDelete: ImageButton
    private lateinit var btnFavorite: ImageButton

    // ❤️ Store favorites persistently
    private val favorites = mutableSetOf<String>()
    private val prefs by lazy { getSharedPreferences("gallery_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bind views
        viewPager = binding.viewPager
        photoInfo = binding.photoInfo
        btnShare = binding.btnShare
        btnDelete = binding.btnDelete
        btnFavorite = binding.btnFavorite

        // Load favorites from storage
        favorites.addAll(prefs.getStringSet("favorites", emptySet()) ?: emptySet())

        // Load all saved images
        loadImages()

        // Adapter setup
        adapter = GalleryAdapter(imageUris)
        viewPager.adapter = adapter

        // Initialize first photo info
        if (imageUris.isNotEmpty()) updatePhotoInfo(0)

        // Swipe listener
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePhotoInfo(position)
            }
        })

        // ✨ Tap anywhere on the image to hide/show control bar
        viewPager.setOnClickListener {
            val controls = binding.galleryControls
            controls.animate()
                .alpha(if (controls.alpha == 1f) 0f else 1f)
                .setDuration(200)
                .start()
        }

        // --- Button Handlers ---
        btnShare.setOnClickListener { shareImage(getCurrentUri()) }
        btnDelete.setOnClickListener { deleteImage(getCurrentUri()) }
        btnFavorite.setOnClickListener { toggleFavorite(getCurrentUri()) }
    }

    private fun getCurrentUri(): Uri = imageUris[viewPager.currentItem]

    // 🖼 Load image URIs from MediaStore
    private fun loadImages() {
        imageUris.clear()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("%OpenCVFilterApp%"),
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                imageUris.add(uri)
            }
        }
    }

    // 🧠 Display EXIF metadata
    private fun updatePhotoInfo(position: Int) {
        val uri = imageUris[position]
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val exif = inputStream?.let { ExifInterface(it) }

            val filterInfo = exif?.getAttribute(ExifInterface.TAG_USER_COMMENT)
                ?: "Filter Info Unavailable"
            val timestamp = exif?.getAttribute(ExifInterface.TAG_DATETIME)
                ?: "Unknown Date"

            photoInfo.text = "🖋 $filterInfo • $timestamp"

            // Update favorite icon
            val key = uri.toString()
            if (favorites.contains(key))
                btnFavorite.setImageResource(R.drawable.ic_favorite)
            else
                btnFavorite.setImageResource(R.drawable.ic_favorite_border)

        } catch (e: Exception) {
            e.printStackTrace()
            photoInfo.text = "❌ Metadata not found"
        }
    }

    // 📤 Share photo
    private fun shareImage(uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Image via"))
    }

    // 🗑️ Delete photo
    private fun deleteImage(uri: Uri) {
        try {
            contentResolver.delete(uri, null, null)
            val position = viewPager.currentItem
            imageUris.removeAt(position)
            adapter.notifyItemRemoved(position)
            Toast.makeText(this, "🗑️ Image deleted", Toast.LENGTH_SHORT).show()

            if (imageUris.isEmpty()) {
                finish()
            } else {
                val newPos = position.coerceAtMost(imageUris.size - 1)
                viewPager.setCurrentItem(newPos, true)
                updatePhotoInfo(newPos)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ❤️ Favorite / Unfavorite (Persistent + Animated)
    private fun toggleFavorite(uri: Uri) {
        val key = uri.toString()
        if (favorites.contains(key)) {
            favorites.remove(key)
            btnFavorite.setImageResource(R.drawable.ic_favorite_border)
            animateHeart()
            Toast.makeText(this, "💔 Removed from favorites", Toast.LENGTH_SHORT).show()
        } else {
            favorites.add(key)
            btnFavorite.setImageResource(R.drawable.ic_favorite)
            animateHeart()
            Toast.makeText(this, "❤️ Added to favorites", Toast.LENGTH_SHORT).show()
        }

        // Save favorites persistently
        prefs.edit().putStringSet("favorites", favorites).apply()
    }

    // 💖 Smooth heart animation
    private fun animateHeart() {
        btnFavorite.animate()
            .scaleX(1.3f)
            .scaleY(1.3f)
            .setDuration(120)
            .withEndAction {
                btnFavorite.animate().scaleX(1f).scaleY(1f).setDuration(120)
            }.start()
    }
}