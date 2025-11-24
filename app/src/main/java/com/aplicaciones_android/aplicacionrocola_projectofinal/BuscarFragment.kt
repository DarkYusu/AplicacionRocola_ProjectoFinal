package com.aplicaciones_android.aplicacionrocola_projectofinal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.regex.Pattern
import kotlin.concurrent.thread

class BuscarFragment : Fragment() {

    private val API_KEY = "AIzaSyC0LLj7FwpsNE0h2hmtxf6AnKqEKZX6rBU"

    private lateinit var input: SearchView
    private lateinit var chipGroup: ChipGroup
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: SongItemAdapter
    private lateinit var progress: ProgressBar
    private lateinit var statusText: TextView

    // pagination / state
    private var currentQuery: String = ""
    private var nextPageToken: String? = null
    private var isLoading = false

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

        // pagination listener
        val layoutManager = recycler.layoutManager as GridLayoutManager
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                if (dy <= 0) return
                val total = layoutManager.itemCount
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                if (!isLoading && nextPageToken != null && total - lastVisible <= 6) {
                    loadMore()
                }
            }
        })

        // SearchView listener
        input.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    currentQuery = query.trim()
                    nextPageToken = null
                    adapter.update(emptyList())
                    performSearch(currentQuery, null)
                    input.clearFocus()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        // chips: buscar como "música <categoría>" para priorizar resultados musicales
        val chips = listOf(R.id.chip_rock, R.id.chip_metal, R.id.chip_punk, R.id.chip_trash)
        // Las chips lanzan búsquedas propias pero no modifican el SearchView (independencia)
        chips.forEach { id ->
            val chip = view.findViewById<Chip>(id)
            chip.setOnClickListener {
                val cat = chip.text.toString()
                val queryText = "música $cat"
                // No tocar `input` aquí: SearchView y chips son independientes
                currentQuery = queryText
                nextPageToken = null
                adapter.update(emptyList())
                performSearch(currentQuery, null)
            }
        }
    }

    private fun loadMore() {
        nextPageToken?.let { token ->
            performSearch(currentQuery, token)
        }
    }

    private fun performSearch(query: String, pageToken: String?) {
        if (isLoading) return
        isLoading = true
        activity?.runOnUiThread { progress.visibility = View.VISIBLE; statusText.visibility = View.GONE }

        val encoded = URLEncoder.encode(query, "UTF-8")
        val pageParam = if (pageToken.isNullOrBlank()) "" else "&pageToken=$pageToken"
        val searchUrl = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=15&q=$encoded$pageParam&key=$API_KEY"

        thread {
            try {
                val client = OkHttpClient()
                val req = Request.Builder().url(searchUrl).build()
                val resp = client.newCall(req).execute()
                val body = resp.body?.string() ?: ""
                val json = JSONObject(body)
                val arr = json.optJSONArray("items")
                val next = if (json.has("nextPageToken")) json.optString("nextPageToken") else null
                val rawItems = mutableListOf<JSONObject>()
                val videoIds = mutableListOf<String>()

                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val itobj = arr.getJSONObject(i)
                        val idObj = itobj.optJSONObject("id")
                        val videoId = idObj?.optString("videoId") ?: ""
                        if (videoId.isNotEmpty()) {
                            rawItems.add(itobj)
                            videoIds.add(videoId)
                        }
                    }
                }

                val items = mutableListOf<SongItem>()

                if (videoIds.isNotEmpty()) {
                    val idsParam = videoIds.joinToString(",")
                    val videosUrl = "https://www.googleapis.com/youtube/v3/videos?part=snippet,contentDetails&id=$idsParam&key=$API_KEY"
                    val req2 = Request.Builder().url(videosUrl).build()
                    val resp2 = client.newCall(req2).execute()
                    val body2 = resp2.body?.string() ?: ""
                    val j2 = JSONObject(body2)
                    val arr2 = j2.optJSONArray("items")

                    val categoryMap = mutableMapOf<String, String>()
                    val durationMap = mutableMapOf<String, Long>()

                    if (arr2 != null) {
                        for (i in 0 until arr2.length()) {
                            val v = arr2.getJSONObject(i)
                            val vid = v.optString("id")
                            val snippet = v.optJSONObject("snippet")
                            val cat = snippet?.optString("categoryId") ?: ""
                            val content = v.optJSONObject("contentDetails")
                            val durationIso = content?.optString("duration") ?: ""
                            val durationSec = parseIso8601Duration(durationIso)
                            if (vid.isNotEmpty()) {
                                categoryMap[vid] = cat
                                durationMap[vid] = durationSec
                            }
                        }
                    }

                    val blacklist = listOf(
                        "compilation", "compilación", "compilado", "compilations", "full album", "full-album",
                        "complete album", "best of", "greatest hits", "various artists", "varios artistas",
                        "playlist", "mix", "medley", "megamix", "dj set", "continuous", "hour", "hours",
                        "full album", "full album stream", "album stream",
                        "greatest hits", "collection", "various artists", "various-artists"
                    )

                    // Separar resultados VEVO y no-VEVO para preferir VEVO si hay disponibles
                    val vevoItems = mutableListOf<SongItem>()
                    val otherItems = mutableListOf<SongItem>()

                    for (obj in rawItems) {
                        val idObj = obj.optJSONObject("id")
                        val videoId = idObj?.optString("videoId") ?: ""
                        val catId = categoryMap[videoId] ?: ""
                        val dur = durationMap[videoId] ?: 0L

                        if (catId != "10") continue
                        if (dur < 60 || dur > 600) continue

                        val snippet = obj.getJSONObject("snippet")
                        val rawTitle = snippet.optString("title")
                        val channel = snippet.optString("channelTitle")
                        val videoUrl = "https://www.youtube.com/watch?v=$videoId"
                        val thumbnails = snippet.optJSONObject("thumbnails")
                        var thumb = ""
                        if (thumbnails != null) {
                            thumb = when {
                                thumbnails.has("high") -> thumbnails.getJSONObject("high").optString("url")
                                thumbnails.has("medium") -> thumbnails.getJSONObject("medium").optString("url")
                                thumbnails.has("default") -> thumbnails.getJSONObject("default").optString("url")
                                else -> ""
                            }
                        }

                        val titleLower = rawTitle.lowercase()
                        val channelLower = channel.lowercase()
                        var excluded = false
                        for (term in blacklist) {
                            if (titleLower.contains(term) || channelLower.contains(term)) {
                                excluded = true
                                break
                            }
                        }
                        if (excluded) continue

                        val (songName, artistName) = parseTitle(rawTitle, channel)
                        val songItem = SongItem(thumb, songName, artistName, videoUrl)

                        if (channelLower.contains("vevo")) {
                            vevoItems.add(songItem)
                        } else {
                            otherItems.add(songItem)
                        }
                    }

                    // Si hay resultados VEVO, preferirlos (muestran versiones oficiales); si no, mostrar los otros
                    if (vevoItems.isNotEmpty()) {
                        items.addAll(vevoItems)
                    } else {
                        items.addAll(otherItems)
                    }
                }

                activity?.runOnUiThread {
                    // Update adapter and pagination state
                    if (pageToken.isNullOrBlank()) {
                        adapter.update(items)
                    } else {
                        adapter.append(items)
                    }
                    nextPageToken = next
                    isLoading = false
                    progress.visibility = View.GONE
                    statusText.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
                    if (adapter.itemCount == 0) {
                        statusText.text = getString(R.string.no_songs_found)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    isLoading = false
                    progress.visibility = View.GONE
                    statusText.visibility = View.VISIBLE
                    statusText.text = getString(R.string.error_searching_songs)
                }
            }
        }
    }

    private fun parseTitle(rawTitle: String, channel: String): Pair<String, String> {
        // Patrones comunes: "Artist - Song" o "Song - Artist"
        val separators = listOf(" - ", " – ", " — ", " | ")
        for (sep in separators) {
            if (rawTitle.contains(sep)) {
                val parts = rawTitle.split(sep, limit = 2)
                if (parts.size == 2) {
                    val a = parts[0].trim()
                    val b = parts[1].trim()
                    // Si el canal coincide con alguna parte, usar canal como artista
                    return when {
                        channel.contains(a, ignoreCase = true) -> Pair(b, channel)
                        channel.contains(b, ignoreCase = true) -> Pair(a, channel)
                        // Si la parte izquierda es corta (probablemente artista), asumir Artist - Song
                        a.length < b.length -> Pair(b, a)
                        else -> Pair(a, b)
                    }
                }
            }
        }
        // Si no hay separador, usar title como song y channel como artista
        return Pair(rawTitle, channel)
    }

    private fun parseIso8601Duration(duration: String?): Long {
        if (duration.isNullOrEmpty()) return 0L
        // ISO8601 example: PT1H2M3S or PT3M45S or PT45S
        val pattern = Pattern.compile("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")
        val matcher = pattern.matcher(duration)
        if (!matcher.matches()) return 0L
        var hours = 0L
        var minutes = 0L
        var seconds = 0L
        matcher.group(1)?.toLongOrNull()?.let { hours = it }
        matcher.group(2)?.toLongOrNull()?.let { minutes = it }
        matcher.group(3)?.toLongOrNull()?.let { seconds = it }
        return hours * 3600 + minutes * 60 + seconds
    }
}
