package com.example.opencvfilterapp

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.opencvfilterapp.databinding.ActivityFullscreenImageBinding

class FullscreenImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullscreenImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullscreenImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar for immersive experience
        supportActionBar?.hide()

        val uriString = intent.getStringExtra("imageUri")
        val uri = uriString?.let { Uri.parse(it) }

        // Load image smoothly
        Glide.with(this)
            .load(uri)
            .placeholder(R.drawable.image_sample)
            .fitCenter()
            .into(binding.fullscreenImage)

        // Tap anywhere to close
        binding.fullscreenImage.setOnClickListener { finishAfterTransition() }
    }
}