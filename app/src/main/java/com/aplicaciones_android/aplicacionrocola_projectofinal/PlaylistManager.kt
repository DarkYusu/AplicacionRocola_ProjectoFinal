package com.aplicaciones_android.aplicacionrocola_projectofinal

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object PlaylistManager {
    private val _songs = MutableLiveData<List<SongItem>>(emptyList())
    val songs: LiveData<List<SongItem>> = _songs

    fun addSong(song: SongItem) {
        val current = _songs.value ?: emptyList()
        if (current.any { it.title == song.title && it.channel == song.channel }) {
            return
        }
        _songs.postValue(current + song)
    }

    fun removeSong(song: SongItem) {
        val current = _songs.value ?: emptyList()
        _songs.postValue(current.filterNot { it.title == song.title && it.channel == song.channel })
    }

    fun clear() {
        _songs.postValue(emptyList())
    }
}

