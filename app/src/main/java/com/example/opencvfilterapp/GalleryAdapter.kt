package com.example.opencvfilterapp

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.opencvfilterapp.databinding.ItemGalleryImageBinding

class GalleryAdapter(
    private val images: List<Uri>,
    private val onImageClick: (Uri, Int) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemGalleryImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Handle clicks safely with position check
            binding.imageView.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < images.size) {
                    onImageClick(images[pos], pos)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGalleryImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = images[position]

        // Debug log for diagnostics
        Log.d("GalleryAdapter", "Loading URI: $uri (position=$position)")

        holder.binding.imageView.visibility = View.VISIBLE

        // Load image with smooth fade, cache, and thumbnail optimization
        Glide.with(holder.binding.imageView.context)
            .load(uri)
            .placeholder(android.R.color.darker_gray)
            .error(android.R.color.darker_gray)
            .centerCrop() // ensures proper grid crop
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .thumbnail(0.25f) // quick preview
            .into(holder.binding.imageView)
    }

    override fun getItemCount(): Int = images.size
}