package com.aplicaciones_android.aplicacionrocola_projectofinal.ui.playlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.aplicaciones_android.aplicacionrocola_projectofinal.R
import com.aplicaciones_android.aplicacionrocola_projectofinal.data.model.SongItem

class PlaylistAdapter(private var items: List<SongItem>) : RecyclerView.Adapter<PlaylistAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.playlist_item_image)
        val title: TextView = view.findViewById(R.id.playlist_item_title)
        val artist: TextView = view.findViewById(R.id.playlist_item_artist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist_song, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.artist.text = item.channel
        holder.image.load(item.thumbnail) {
            placeholder(R.mipmap.ic_launcher)
            error(R.mipmap.ic_launcher)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submit(data: List<SongItem>) {
        items = data
        notifyDataSetChanged()
    }
}
