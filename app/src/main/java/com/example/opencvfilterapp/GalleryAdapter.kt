package com.example.opencvfilterapp

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.opencvfilterapp.databinding.ItemGalleryImageBinding

class GalleryAdapter(
    private val images: List<Uri>,
    private val onClick: (Uri) -> Unit
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
        holder.binding.imageView.setImageURI(uri)
        holder.binding.imageView.setOnClickListener { onClick(uri) }
    }

    override fun getItemCount() = images.size
}