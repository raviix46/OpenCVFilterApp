package com.example.opencvfilterapp

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.opencvfilterapp.databinding.ItemGalleryImageBinding

/**
 * Adapter for the full-screen image viewer inside ViewPager2.
 * Handles smooth transitions and optional zoom support.
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

        // 🌀 Load image using Glide with smooth fade-in animation
        Glide.with(holder.binding.imageView.context)
            .load(uri)
            .placeholder(android.R.color.darker_gray)
            .error(android.R.color.darker_gray)
            .fitCenter()
            .transition(
                com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(300)
            ) // ✨ smooth fade-in
            .into(holder.binding.imageView)

        // 📸 Set default scale behavior
        holder.binding.imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        // 🔍 OPTIONAL: Enable pinch-to-zoom (requires PhotoView)
        // Uncomment the next lines if you replace ImageView with PhotoView in item_gallery_image.xml
        // if (holder.binding.imageView is PhotoView) {
        //     (holder.binding.imageView as PhotoView).setZoomable(true)
        //     (holder.binding.imageView as PhotoView).maximumScale = 4.0f
        // }
    }

    override fun getItemCount(): Int = images.size
}