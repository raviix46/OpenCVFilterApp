package com.example.opencvfilterapp

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.opencvfilterapp.databinding.ActivityFullscreenImageBinding

class FullscreenImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullscreenImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullscreenImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uri = Uri.parse(intent.getStringExtra("imageUri"))
        binding.fullscreenImage.setImageURI(uri)

        binding.fullscreenImage.setOnClickListener { finish() }
    }
}