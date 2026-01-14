package com.example.parkingmate

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.parkingmate.databinding.ItemGalleryBinding
import java.io.File

class GalleryAdapter(
    private val images: MutableList<File>,
    private val onDeleteClick: (File, Int) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

    // ViewHolder za svaku sliku u galeriji
    inner class GalleryViewHolder(val binding: ItemGalleryBinding) :
        RecyclerView.ViewHolder(binding.root)

    // Kreira novi ViewHolder kada je potrebno
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val binding = ItemGalleryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GalleryViewHolder(binding)
    }

    // Povezuje podatke slike sa ViewHolder-om
    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        val file = images[position]
        // Postavlja sliku u ImageView
        holder.binding.imageView.setImageURI(Uri.fromFile(file))

        // Postavlja klik listener za otvaranje detalja slike
        holder.binding.root.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, PhotoDetailActivity::class.java)
            intent.putExtra("imagePath", file.absolutePath)
            context.startActivity(intent)
        }

        // Postavlja long click listener za brisanje slike
        holder.binding.root.setOnLongClickListener {
            onDeleteClick(file, position)
            true
        }
    }

    // Vraća ukupan broj slika u galeriji
    override fun getItemCount() = images.size

    // Uklanja sliku iz liste
    fun removeItem(position: Int) {
        if (position in 0 until images.size) {
            images.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, images.size)
        }
    }
}