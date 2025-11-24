package com.aplicaciones_android.aplicacionrocola_projectofinal

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.aplicaciones_android.aplicacionrocola_projectofinal.data.model.SongItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

object PlaylistManager {
    private const val TAG = "PlaylistManager"
    private const val PLAYLISTS_COLLECTION = "playlists"
    private const val SONGS_COLLECTION = "songs"
    private const val DEFAULT_PLAYLIST_ID = "default"

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val playlistRef
        get() = firestore
            .collection(PLAYLISTS_COLLECTION)
            .document(DEFAULT_PLAYLIST_ID)
            .collection(SONGS_COLLECTION)

    private val _songs = MutableLiveData<List<SongItem>>(emptyList())
    val songs: LiveData<List<SongItem>> = _songs

    private var listenerRegistration: ListenerRegistration? = null
    private var initialized = false
    private var appContext: Context? = null

    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        FirebaseUtils.ensureInitialized(appContext!!)
        listenerRegistration = playlistRef
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando playlist", error)
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents ?: emptyList()
                val newSongs = docs.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val channel = doc.getString("channel") ?: return@mapNotNull null
                    val thumbnail = doc.getString("thumbnail") ?: ""
                    val url = doc.getString("videoUrl") ?: ""
                    SongItem(thumbnail, title, channel, url)
                }
                _songs.postValue(newSongs)
            }
        initialized = true
    }

    private fun ensureInitialized() {
        if (!initialized) {
            appContext?.let { initialize(it) } ?: Log.w(TAG, "PlaylistManager no ha sido inicializado aún")
        }
    }

    fun addSong(song: SongItem) {
        ensureInitialized()
        val current = _songs.value ?: emptyList()
        if (current.any { it.title == song.title && it.channel == song.channel }) {
            return
        }
        val docId = docIdFor(song)
        playlistRef.document(docId)
            .set(song.toMap())
            .addOnFailureListener { Log.e(TAG, "Error guardando canción", it) }
    }

    fun removeSong(song: SongItem) {
        ensureInitialized()
        val docId = docIdFor(song)
        playlistRef.document(docId)
            .delete()
            .addOnFailureListener { Log.e(TAG, "Error eliminando canción", it) }
    }

    fun clear() {
        ensureInitialized()
        playlistRef.get()
            .addOnSuccessListener { snap ->
                val batch = firestore.batch()
                snap.documents.forEach { batch.delete(it.reference) }
                batch.commit().addOnFailureListener { Log.e(TAG, "Error limpiando playlist", it) }
            }
            .addOnFailureListener { Log.e(TAG, "Error obteniendo playlist", it) }
    }

    fun shutdown() {
        listenerRegistration?.remove()
        listenerRegistration = null
        initialized = false
    }

    private fun docIdFor(song: SongItem): String {
        val raw = "${song.title}-${song.channel}".lowercase()
        val sanitized = raw.replace("[^a-z0-9]+".toRegex(), "_").trim('_')
        return if (sanitized.isNotEmpty()) sanitized else raw.hashCode().toString()
    }

    private fun SongItem.toMap(): Map<String, Any> = mapOf(
        "title" to title,
        "channel" to channel,
        "thumbnail" to thumbnail,
        "videoUrl" to videoUrl,
        "createdAt" to System.currentTimeMillis()
    )
}
