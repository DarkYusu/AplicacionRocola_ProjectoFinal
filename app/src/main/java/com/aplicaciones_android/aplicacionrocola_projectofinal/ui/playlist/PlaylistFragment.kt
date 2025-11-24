package com.aplicaciones_android.aplicacionrocola_projectofinal.ui.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.aplicaciones_android.aplicacionrocola_projectofinal.PlaylistManager
import com.aplicaciones_android.aplicacionrocola_projectofinal.R
import com.aplicaciones_android.aplicacionrocola_projectofinal.data.model.SongItem
import com.google.android.material.button.MaterialButton

class PlaylistFragment : Fragment() {
    private lateinit var headerCard: View
    private lateinit var headerImage: ImageView
    private lateinit var headerTitle: TextView
    private lateinit var headerArtist: TextView
    private lateinit var emptyView: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: PlaylistAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        headerCard = view.findViewById(R.id.header_card)
        headerImage = view.findViewById(R.id.header_image)
        headerTitle = view.findViewById(R.id.header_song_title)
        headerArtist = view.findViewById(R.id.header_song_artist)
        emptyView = view.findViewById(R.id.playlist_empty)
        recycler = view.findViewById(R.id.playlist_recycler)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = PlaylistAdapter(emptyList())
        recycler.adapter = adapter

        PlaylistManager.songs.observe(viewLifecycleOwner) { songs ->
            if (songs.isNullOrEmpty()) {
                headerCard.visibility = View.GONE
                recycler.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            } else {
                emptyView.visibility = View.GONE
                headerCard.visibility = View.VISIBLE
                headerTitle.text = songs.first().title
                headerArtist.text = songs.first().channel
                headerImage.load(songs.first().thumbnail) {
                    placeholder(R.mipmap.ic_launcher)
                    error(R.mipmap.ic_launcher)
                }

                val rest = if (songs.size > 1) songs.drop(1) else emptyList()
                recycler.visibility = if (rest.isEmpty()) View.GONE else View.VISIBLE
                adapter.submit(rest)
            }
        }
    }
}
