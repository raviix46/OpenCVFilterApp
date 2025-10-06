package com.example.opencvfilterapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.opencvfilterapp.databinding.ActivityGalleryBinding

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private val imageUris = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView setup
        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)
        val adapter = GalleryAdapter(imageUris) { uri ->
            val intent = Intent(this, FullscreenImageActivity::class.java)
            intent.putExtra("imageUri", uri.toString())
            startActivity(intent)
        }
        binding.recyclerView.adapter = adapter

        // Load all saved images
        loadImages()
        adapter.notifyDataSetChanged()
    }

    private fun loadImages() {
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
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                imageUris.add(uri)
            }
        }
    }
}