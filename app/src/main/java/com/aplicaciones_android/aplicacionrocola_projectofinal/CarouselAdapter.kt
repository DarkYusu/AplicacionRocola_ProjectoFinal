package com.aplicaciones_android.aplicacionrocola_projectofinal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aplicaciones_android.aplicacionrocola_projectofinal.model.CarouselItem

class CarouselAdapter(private val items: List<CarouselItem>) : RecyclerView.Adapter<CarouselAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.carousel_item_image)
        val title: TextView = view.findViewById(R.id.carousel_item_title)
        val band: TextView = view.findViewById(R.id.carousel_item_band)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_carousel, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.image.setImageResource(item.imageResId)
        holder.title.text = item.title
        holder.band.text = item.bandName ?: "Banda - Desconocida"
    }

    override fun getItemCount(): Int = items.size
}

