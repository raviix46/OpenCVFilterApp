package com.example.opencvfilterapp

import android.content.ContentUris
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.opencvfilterapp.databinding.ActivityGalleryBinding

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private val imageUris = mutableListOf<Uri>()
    private lateinit var adapter: GalleryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🖼️ Setup 3-column grid layout
        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.visibility = View.VISIBLE

        // 🧩 Setup adapter with click handler
        adapter = GalleryAdapter(imageUris) { uri, position ->
            val intent = Intent(this@GalleryActivity, FullscreenImageActivity::class.java).apply {
                putStringArrayListExtra(
                    "allImageUris",
                    ArrayList(imageUris.map { it.toString() })
                )
                putExtra("startPosition", position)
            }
            // ✅ Launch fullscreen with result listener
            startActivityForResult(intent, 101)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        binding.recyclerView.adapter = adapter

        // 🗂️ Load and display images
        loadImages()
        adapter.notifyDataSetChanged()

        // 🔄 Optional: rescan MediaStore for new captures
        rescanMedia()
    }

    // 🧠 Reload when returning from fullscreen (after deletion)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101 && resultCode == RESULT_OK) {
            val deletedUri = data?.getStringExtra("deletedUri")
            if (deletedUri != null) {
                // 🗑 Remove the deleted image from list
                val removed = imageUris.removeIf { it.toString() == deletedUri }
                if (removed) {
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this, "🗑️ Gallery updated", Toast.LENGTH_SHORT).show()
                } else {
                    // Reload completely if list mismatch
                    loadImages()
                    adapter.notifyDataSetChanged()
                }
            } else {
                // If no URI sent, just refresh gallery
                loadImages()
                adapter.notifyDataSetChanged()
            }
        }
    }

    // 🗂️ Load images stored in media folders
    private fun loadImages() {
        imageUris.clear()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.RELATIVE_PATH
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataColumn = it.getColumnIndex(MediaStore.Images.Media.DATA)
            val relPathColumn = it.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)

            while (it.moveToNext()) {
                val absPath = if (dataColumn != -1) it.getString(dataColumn) else ""
                val relPath = if (relPathColumn != -1) it.getString(relPathColumn) else ""

                if (absPath.contains("OpenCVFilterApp", ignoreCase = true) ||
                    relPath.contains("OpenCVFilterApp", ignoreCase = true) ||
                    absPath.contains("Pictures", ignoreCase = true) ||
                    absPath.contains("DCIM", ignoreCase = true)
                ) {
                    val id = it.getLong(idColumn)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                    )
                    imageUris.add(uri)
                }
            }
        }

        if (imageUris.isEmpty()) {
            Toast.makeText(this, "⚠️ No saved images found!", Toast.LENGTH_SHORT).show()
        }
    }

    // 🔄 Force MediaStore rescan for new captures
    private fun rescanMedia() {
        try {
            MediaScannerConnection.scanFile(
                this,
                arrayOf("/storage/emulated/0/Pictures/OpenCVFilterApp"),
                arrayOf("image/jpeg")
            ) { _, _ ->
                runOnUiThread {
                    loadImages()
                    adapter.notifyDataSetChanged()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}