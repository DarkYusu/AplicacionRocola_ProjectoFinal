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
import com.aplicaciones_android.aplicacionrocola_projectofinal.data.model.SongItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.android.material.bottomnavigation.BottomNavigationView
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.regex.Pattern
import kotlin.concurrent.thread

class FragmentInicio : Fragment() {
    // Cambia a `false` si no quieres datos de ejemplo cuando Firestore/YouTube esté vacío
    private val SHOW_SAMPLE_IF_EMPTY = false

    // YouTube API key (se usa también en BuscarFragment). Si tienes un lugar central, mejor moverlo.
    private val YOUTUBE_API_KEY = "AIzaSyBfJnGzFenvo58MxZZeiLNLwcmHYiWbmNw"

    private lateinit var menuImage: ImageView
    private lateinit var rocolaImage: ImageView
    private lateinit var recommendedRecycler: RecyclerView
    private lateinit var songAdapter: SongItemAdapter
    private lateinit var recommendedStatus: TextView

    private var lastRecommended: List<SongItem> = emptyList()

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
        loadLatestImage("menu_images", menuImage, R.id.tag_menu_image_url)
        loadLatestImage("rocola_images", rocolaImage, R.id.tag_rocola_image_url)
        menuImage.setOnClickListener { navigateTo(R.id.nav_menu) }
        rocolaImage.setOnClickListener { navigateTo(R.id.nav_buscar) }

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

    private fun loadLatestImage(collection: String, targetView: ImageView, tagId: Int) {
        val db = FirebaseFirestore.getInstance()
        db.collection(collection)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snaps ->
                if (!snaps.isEmpty) {
                    val doc = snaps.documents[0]
                    val url = doc.getString("imageUrl")
                    if (!url.isNullOrBlank() && targetView.getTag(tagId) != url) {
                        targetView.setTag(tagId, url)
                        targetView.load(url) {
                            crossfade(true)
                            placeholder(R.drawable.bg_image_placeholder)
                            error(R.drawable.bg_image_placeholder)
                        }
                    }
                }
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
                val items = mutableListOf<SongItem>()

                if (arr != null) {
                    val vevoItems = mutableListOf<SongItem>()
                    val otherItems = mutableListOf<SongItem>()

                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val idObj = obj.optJSONObject("id")
                        val videoId = idObj?.optString("videoId") ?: continue
                        val snippet = obj.optJSONObject("snippet") ?: continue

                        val rawTitle = htmlDecode(snippet.optString("title"))
                        val channel = htmlDecode(snippet.optString("channelTitle"))
                        val videoUrl = "https://www.youtube.com/watch?v=$videoId"

                        val thumbnails = snippet.optJSONObject("thumbnails")
                        val thumb = when {
                            thumbnails?.has("high") == true -> thumbnails.getJSONObject("high").optString("url")
                            thumbnails?.has("medium") == true -> thumbnails.getJSONObject("medium").optString("url")
                            thumbnails?.has("default") == true -> thumbnails.getJSONObject("default").optString("url")
                            else -> ""
                        }

                        val (songName, artistName) = parseTitle(rawTitle, channel)
                        val bucket = if (channel.contains("VEVO", ignoreCase = true)) vevoItems else otherItems
                        bucket.add(SongItem(thumb, songName, artistName, videoUrl))
                    }

                    activity?.runOnUiThread {
                        val ordered = if (vevoItems.isNotEmpty()) vevoItems + otherItems else otherItems
                        if (ordered.isNotEmpty()) {
                            lastRecommended = ordered
                            songAdapter.update(ordered)
                            recommendedStatus.visibility = View.GONE
                            return@runOnUiThread
                        }
                        if (items.isEmpty()) {
                            if (lastRecommended.isEmpty()) {
                                recommendedStatus.text = getString(R.string.no_songs_found)
                                recommendedStatus.visibility = View.VISIBLE
                            } else {
                                recommendedStatus.visibility = View.GONE
                            }
                        }
                    }

                }

            } catch (e: Exception) {
                Log.e("FragmentInicio", "error fetching from YouTube API", e)
                activity?.runOnUiThread {
                    if (lastRecommended.isEmpty()) {
                        recommendedStatus.text = getString(R.string.error_searching_songs)
                        recommendedStatus.visibility = View.VISIBLE
                    } else {
                        recommendedStatus.visibility = View.GONE
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

    private fun navigateTo(tabId: Int) {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.selectedItemId = tabId
    }
}
