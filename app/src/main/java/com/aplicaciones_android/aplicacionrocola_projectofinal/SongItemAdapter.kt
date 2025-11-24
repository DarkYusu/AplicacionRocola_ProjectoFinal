package com.aplicaciones_android.aplicacionrocola_projectofinal

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

data class SongItem(val thumbnail: String, val title: String, val channel: String, val videoUrl: String)

class SongItemAdapter(private var items: MutableList<SongItem>, private val onAddToPlaylist: ((SongItem) -> Unit)? = null) : RecyclerView.Adapter<SongItemAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.song_image)
        val title: TextView = view.findViewById(R.id.song_title)
        val artist: TextView = view.findViewById(R.id.song_artist)
        val addButton: View = view.findViewById(R.id.add_to_playlist_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        try {
            holder.image.load(item.thumbnail) {
                crossfade(true)
                placeholder(R.mipmap.ic_launcher)
                error(R.mipmap.ic_launcher)
            }
        } catch (e: Exception) {
            Log.w("SongItemAdapter", "failed to load thumbnail: ${e.message}")
            holder.image.setImageResource(R.mipmap.ic_launcher)
        }
        holder.title.text = item.title
        holder.artist.text = item.channel
        holder.addButton.setOnClickListener { onAddToPlaylist?.invoke(item) }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<SongItem>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    fun append(more: List<SongItem>) {
        val start = items.size
        items.addAll(more)
        notifyItemRangeInserted(start, more.size)
    }
}
