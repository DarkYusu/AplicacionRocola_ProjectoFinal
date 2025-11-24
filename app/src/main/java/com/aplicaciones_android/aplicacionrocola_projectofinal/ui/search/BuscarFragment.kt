package com.aplicaciones_android.aplicacionrocola_projectofinal.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aplicaciones_android.aplicacionrocola_projectofinal.PlaylistManager
import com.aplicaciones_android.aplicacionrocola_projectofinal.R
import com.aplicaciones_android.aplicacionrocola_projectofinal.ui.common.SongItemAdapter
import com.aplicaciones_android.aplicacionrocola_projectofinal.data.remote.YoutubeRemoteDataSource
import com.aplicaciones_android.aplicacionrocola_projectofinal.data.repository.SearchRepository
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BuscarFragment : Fragment() {

    private lateinit var input: SearchView
    private lateinit var chipGroup: ChipGroup
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: SongItemAdapter
    private lateinit var progress: ProgressBar
    private lateinit var statusText: TextView

    private val viewModel: BuscarViewModel by viewModels {
        BuscarViewModelFactory(
            SearchRepository(YoutubeRemoteDataSource(getString(R.string.youtube_api_key)))
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        input = view.findViewById(R.id.search_view)
        chipGroup = view.findViewById(R.id.chip_group)
        recycler = view.findViewById(R.id.search_results)
        progress = view.findViewById(R.id.progress)
        statusText = view.findViewById(R.id.status_text)

        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = SongItemAdapter(mutableListOf()) { song ->
            PlaylistManager.addSong(song)
            Toast.makeText(requireContext(), getString(R.string.added_to_playlist), Toast.LENGTH_SHORT).show()
        }
        recycler.adapter = adapter

        val layoutManager = recycler.layoutManager as GridLayoutManager
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                if (dy <= 0) return
                val total = layoutManager.itemCount
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                if (total - lastVisible <= 6) {
                    viewModel.loadNextPage()
                }
            }
        })

        input.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    viewModel.search(query)
                    input.clearFocus()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean = false
        })

        val chips = listOf(R.id.chip_rock, R.id.chip_metal, R.id.chip_punk, R.id.chip_trash)
        chips.forEach { id ->
            val chip = view.findViewById<Chip>(id)
            chip.setOnClickListener { viewModel.searchByChip(chip.text.toString()) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                if (state.error != null) {
                    statusText.visibility = View.VISIBLE
                    statusText.text = getString(R.string.error_searching_songs)
                } else {
                    statusText.visibility = if (state.items.isEmpty()) View.VISIBLE else View.GONE
                    if (state.items.isEmpty()) {
                        statusText.text = getString(R.string.no_songs_found)
                    }
                }
                if (!state.isLoading && state.items.isNotEmpty()) {
                    adapter.update(state.items)
                }
            }
        }
    }
}
