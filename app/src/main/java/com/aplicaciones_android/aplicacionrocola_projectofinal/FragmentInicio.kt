package com.aplicaciones_android.aplicacionrocola_projectofinal

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.regex.Pattern
import kotlin.concurrent.thread

class FragmentInicio : Fragment() {

    // Cambia a `false` si no quieres datos de ejemplo cuando Firestore/YouTube esté vacío
    private val SHOW_SAMPLE_IF_EMPTY = true

    // YouTube API key (se usa también en BuscarFragment). Si tienes un lugar central, mejor moverlo.
    private val YOUTUBE_API_KEY = "AIzaSyC0LLj7FwpsNE0h2hmtxf6AnKqEKZX6rBU"

    private lateinit var menuImage: ImageView
    private lateinit var rocolaImage: ImageView
    private lateinit var recommendedRecycler: RecyclerView
    private lateinit var songAdapter: SongItemAdapter
    private lateinit var recommendedStatus: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_inicio, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Aplicar insets para desplazar contenido bajo la barra de estado
        val root = view.findViewById<View>(R.id.fragment_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        FirebaseUtils.ensureInitialized(requireContext())

        menuImage = view.findViewById(R.id.menu_image)
        rocolaImage = view.findViewById(R.id.rocola_image)
        recommendedRecycler = view.findViewById(R.id.recommended_recycler)
        recommendedStatus = view.findViewById(R.id.recommended_status)

        val adminBtn = view.findViewById<Button>(R.id.admin_button)
        adminBtn.setOnClickListener {
            startActivity(Intent(requireContext(), AdminActivity::class.java))
        }

        // Cargar últimas imágenes subidas por admin para banner y rocola
        loadLatestImage("menu_images") { url ->
            url?.let { menuImage.load(it) }
        }
        loadLatestImage("rocola_images") { url ->
            url?.let { rocolaImage.load(it) }
        }

        // Configurar RecyclerView para mostrar lista vertical de canciones recomendadas
        // Mostrar los items en horizontal (fila) para la sección de recomendados
        recommendedRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recommendedRecycler.setHasFixedSize(true)
        songAdapter = SongItemAdapter(mutableListOf()) { song ->
            PlaylistManager.addSong(song)
            Toast.makeText(requireContext(), getString(R.string.added_to_playlist), Toast.LENGTH_SHORT).show()
        }
        recommendedRecycler.adapter = songAdapter
        recommendedRecycler.visibility = View.VISIBLE

        // Cargar canciones recomendadas desde la API de YouTube
        // Buscar por "rock music" (el filtrado por categoryId se mantiene)
        loadRecommendedFromYouTube("rock music")
    }

    private fun loadLatestImage(collection: String, callback: (String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection(collection)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snaps ->
                if (!snaps.isEmpty) {
                    val doc = snaps.documents[0]
                    val url = doc.getString("imageUrl")
                    callback(url)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    // Nueva función: realizar búsqueda en YouTube y mapear a SongItem
    private fun loadRecommendedFromYouTube(query: String) {
        val TAG = "FragmentInicio"
        Log.d(TAG, "loadRecommendedFromYouTube query=$query")
        recommendedStatus.visibility = View.GONE

        val encoded = URLEncoder.encode(query, "UTF-8")
        val searchUrl = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=12&q=$encoded&key=$YOUTUBE_API_KEY"

        thread {
            try {
                val client = OkHttpClient()
                val req = Request.Builder().url(searchUrl).build()
                val resp = client.newCall(req).execute()
                val body = resp.body?.string() ?: ""
                val json = JSONObject(body)
                val arr = json.optJSONArray("items")
                val videoIds = mutableListOf<String>()
                val rawItems = mutableListOf<JSONObject>()

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
                    val videosUrl = "https://www.googleapis.com/youtube/v3/videos?part=snippet,contentDetails&id=$idsParam&key=$YOUTUBE_API_KEY"
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

                    for (obj in rawItems) {
                        val idObj = obj.optJSONObject("id")
                        val videoId = idObj?.optString("videoId") ?: ""
                        val catId = categoryMap[videoId] ?: ""
                        val dur = durationMap[videoId] ?: 0L

                        // Filtrar sólo música (categoria 10) y duración razonable
                        if (catId != "10") continue
                        if (dur < 60 || dur > 600) continue

                        val snippet = obj.getJSONObject("snippet")
                        val rawTitle = htmlDecode(snippet.optString("title"))
                        val channel = htmlDecode(snippet.optString("channelTitle"))
                        // No aplicar filtro adicional por texto; la query "rock music" ya prioriza el género.
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
                        val songItem = SongItem(thumb, songName, artistName)
                        items.add(songItem)
                    }
                }

                activity?.runOnUiThread {
                    if (items.isEmpty()) {
                        if (SHOW_SAMPLE_IF_EMPTY) {
                            // fallback de ejemplo
                            val pkg = requireContext().packageName
                            val resUri = "android.resource://$pkg/" + R.mipmap.ic_launcher
                            val sample = listOf(
                                SongItem(resUri, "Ejemplo: Canción A", "Artista A"),
                                SongItem(resUri, "Ejemplo: Canción B", "Artista B"),
                                SongItem(resUri, "Ejemplo: Canción C", "Artista C")
                            )
                            songAdapter.update(sample)
                            recommendedStatus.visibility = View.GONE
                        } else {
                            recommendedStatus.text = getString(R.string.no_songs_found)
                            recommendedStatus.visibility = View.VISIBLE
                        }
                    } else {
                        songAdapter.update(items)
                        recommendedStatus.visibility = View.GONE
                    }
                }

            } catch (e: Exception) {
                Log.e("FragmentInicio", "error fetching from YouTube API", e)
                activity?.runOnUiThread {
                    if (SHOW_SAMPLE_IF_EMPTY) {
                        val pkg = requireContext().packageName
                        val resUri = "android.resource://$pkg/" + R.mipmap.ic_launcher
                        val sample = listOf(
                            SongItem(resUri, "Ejemplo: Canción A", "Artista A"),
                            SongItem(resUri, "Ejemplo: Canción B", "Artista B"),
                            SongItem(resUri, "Ejemplo: Canción C", "Artista C")
                        )
                        songAdapter.update(sample)
                        recommendedStatus.visibility = View.GONE
                    } else {
                        recommendedStatus.text = getString(R.string.error_searching_songs)
                        recommendedStatus.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    // Desescapa las entidades HTML comunes que devuelve la API de YouTube.
    private fun htmlDecode(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        var result = text

        val numericEntityRegex = Regex("&#(x?[0-9A-Fa-f]+);")
        result = numericEntityRegex.replace(result) { match ->
            val value = match.groupValues[1]
            try {
                val codePoint = if (value.startsWith("x", ignoreCase = true)) {
                    value.substring(1).toInt(16)
                } else {
                    value.toInt()
                }
                codePoint.toChar().toString()
            } catch (_: Exception) {
                match.value
            }
        }

        return result
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
    }

    private fun parseTitle(rawTitle: String, channel: String): Pair<String, String> {
        val separators = listOf(" - ", " – ", " — ", " | ")
        for (sep in separators) {
            if (rawTitle.contains(sep)) {
                val parts = rawTitle.split(sep, limit = 2)
                if (parts.size == 2) {
                    val a = parts[0].trim()
                    val b = parts[1].trim()
                    return when {
                        channel.contains(a, ignoreCase = true) -> Pair(b, channel)
                        channel.contains(b, ignoreCase = true) -> Pair(a, channel)
                        a.length < b.length -> Pair(b, a)
                        else -> Pair(a, b)
                    }
                }
            }
        }
        return Pair(rawTitle, channel)
    }

    private fun parseIso8601Duration(duration: String?): Long {
        if (duration.isNullOrEmpty()) return 0L
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
