package com.example.opencvfilterapp

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.opencvfilterapp.databinding.ItemGalleryImageBinding

/**
 * Adapter for full-screen image viewer inside ViewPager2
 * Used in GalleryActivity for swipe navigation between photos.
 */
class GalleryAdapter(
    private val images: List<Uri>
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemGalleryImageBinding) :
        RecyclerView.ViewHolder(binding.root)

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

        // Use Glide for smoother image rendering and caching
        Glide.with(holder.binding.imageView.context)
            .load(uri)
            .placeholder(R.drawable.image_sample)
            .error(R.drawable.image_sample)
            .fitCenter()
            .into(holder.binding.imageView)

        // Enable full zoom gestures later if needed
        holder.binding.imageView.scaleType = ImageView.ScaleType.FIT_CENTER
    }

    override fun getItemCount(): Int = images.size
}