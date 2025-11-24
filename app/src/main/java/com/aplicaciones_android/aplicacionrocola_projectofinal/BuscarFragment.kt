package com.aplicaciones_android.aplicacionrocola_projectofinal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.concurrent.thread

class BuscarFragment : Fragment() {

    private val API_KEY = "AIzaSyC0LLj7FwpsNE0h2hmtxf6AnKqEKZX6rBU"

    private lateinit var input: EditText
    private lateinit var button: Button
    private lateinit var chipGroup: ChipGroup
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: SongItemAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        input = view.findViewById(R.id.search_input)
        button = view.findViewById(R.id.search_button)
        chipGroup = view.findViewById(R.id.chip_group)
        recycler = view.findViewById(R.id.search_results)

        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = SongItemAdapter(emptyList())
        recycler.adapter = adapter

        button.setOnClickListener {
            val q = input.text.toString().trim()
            if (q.isNotEmpty()) performSearch(q)
        }

        // chips
        val chips = listOf(R.id.chip_rock, R.id.chip_metal, R.id.chip_punk, R.id.chip_trash)
        chips.forEach { id ->
            val chip = view.findViewById<Chip>(id)
            chip.setOnClickListener {
                val text = chip.text.toString()
                input.setText(text)
                performSearch(text)
            }
        }
    }

    private fun performSearch(query: String) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        // Usar search.list para obtener videoIds, luego validar con videos.list para asegurarnos de categoría Music (10)
        val searchUrl = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=9&q=$encoded&key=$API_KEY"

        thread {
            try {
                val client = OkHttpClient()
                val req = Request.Builder().url(searchUrl).build()
                val resp = client.newCall(req).execute()
                val body = resp.body?.string()
                val rawItems = mutableListOf<JSONObject>()
                val videoIds = mutableListOf<String>()

                body?.let {
                    val json = JSONObject(it)
                    val arr = json.optJSONArray("items")
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
                }

                val items = mutableListOf<SongItem>()

                if (videoIds.isNotEmpty()) {
                    // llamar a videos.list para obtener snippet.categoryId y filtrarlo por 10 (Music)
                    val idsParam = videoIds.joinToString(",")
                    val videosUrl = "https://www.googleapis.com/youtube/v3/videos?part=snippet&id=$idsParam&key=$API_KEY"
                    val req2 = Request.Builder().url(videosUrl).build()
                    val resp2 = client.newCall(req2).execute()
                    val body2 = resp2.body?.string()
                    val categoryMap = mutableMapOf<String, String>() // videoId -> categoryId
                    body2?.let {
                        val j2 = JSONObject(it)
                        val arr2 = j2.optJSONArray("items")
                        if (arr2 != null) {
                            for (i in 0 until arr2.length()) {
                                val v = arr2.getJSONObject(i)
                                val vid = v.optString("id")
                                val snippet = v.optJSONObject("snippet")
                                val cat = snippet?.optString("categoryId") ?: ""
                                if (vid.isNotEmpty()) {
                                    categoryMap[vid] = cat
                                }
                            }
                        }
                    }

                    // ahora recorrer rawItems y agregar solo los que tengan categoryId == "10"
                    for (obj in rawItems) {
                        val idObj = obj.optJSONObject("id")
                        val videoId = idObj?.optString("videoId") ?: ""
                        val catId = categoryMap[videoId] ?: ""
                        if (catId == "10") {
                            val snippet = obj.getJSONObject("snippet")
                            val rawTitle = snippet.optString("title")
                            val channel = snippet.optString("channelTitle")
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

                            val (songName, artistName) = parseTitle(rawTitle, channel)
                            items.add(SongItem(thumb, songName, artistName))
                        }
                    }
                }

                activity?.runOnUiThread {
                    adapter.update(items)
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
}
