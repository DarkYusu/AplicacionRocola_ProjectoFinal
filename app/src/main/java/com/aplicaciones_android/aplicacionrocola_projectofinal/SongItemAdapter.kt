package com.aplicaciones_android.aplicacionrocola_projectofinal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

data class SongItem(val thumbnail: String, val title: String, val channel: String)

class SongItemAdapter(private var items: List<SongItem>) : RecyclerView.Adapter<SongItemAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.song_image)
        val title: TextView = view.findViewById(R.id.song_title)
        val artist: TextView = view.findViewById(R.id.song_artist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.image.load(item.thumbnail) { crossfade(true) }
        holder.title.text = item.title
        holder.artist.text = item.channel
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<SongItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}

