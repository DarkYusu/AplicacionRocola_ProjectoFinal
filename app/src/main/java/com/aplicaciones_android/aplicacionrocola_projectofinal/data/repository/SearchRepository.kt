package com.aplicaciones_android.aplicacionrocola_projectofinal.data.repository

import com.aplicaciones_android.aplicacionrocola_projectofinal.data.model.SongItem
import com.aplicaciones_android.aplicacionrocola_projectofinal.data.remote.YoutubeRemoteDataSource
import com.aplicaciones_android.aplicacionrocola_projectofinal.data.remote.model.YoutubeVideosResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.regex.Pattern

class SearchRepository(
    private val remote: YoutubeRemoteDataSource
) {
    suspend fun search(query: String, pageToken: String?): SearchResult = withContext(Dispatchers.IO) {
        val searchResponse = remote.searchVideos(query, pageToken)
        if (searchResponse.videoIds.isEmpty()) {
            return@withContext SearchResult(emptyList(), searchResponse.nextPageToken)
        }
        val details = remote.fetchVideosDetails(searchResponse.videoIds)
        val items = searchResponse.rawItems
            .mapNotNull { raw -> raw.toSongItem(details) }
        return@withContext SearchResult(items, searchResponse.nextPageToken)
    }

    data class SearchResult(
        val items: List<SongItem>,
        val nextPageToken: String?
    )
}

private fun JSONObject.toSongItem(details: YoutubeVideosResponse): SongItem? {
    val idObj = optJSONObject("id")
    val videoId = idObj?.optString("videoId") ?: return null
    val category = details.categoryById[videoId] ?: return null
    if (category != "10") return null
    val duration = details.durationById[videoId] ?: 0L
    if (duration < 60 || duration > 600) return null

    val snippet = optJSONObject("snippet") ?: return null
    val rawTitle = htmlDecode(snippet.optString("title"))
    val channel = htmlDecode(snippet.optString("channelTitle"))
    if (rawTitle.isBlank() || channel.isBlank()) return null

    val titleLower = rawTitle.lowercase()
    val channelLower = channel.lowercase()
    if (BLACKLIST.any { titleLower.contains(it) || channelLower.contains(it) }) return null

    val thumbnails = snippet.optJSONObject("thumbnails")
    val thumb = when {
        thumbnails?.has("high") == true -> thumbnails.getJSONObject("high").optString("url")
        thumbnails?.has("medium") == true -> thumbnails.getJSONObject("medium").optString("url")
        thumbnails?.has("default") == true -> thumbnails.getJSONObject("default").optString("url")
        else -> ""
    }

    val (song, artist) = parseTitle(rawTitle, channel)
    return SongItem(thumb, song, artist, "https://www.youtube.com/watch?v=$videoId")
}

private fun parseTitle(rawTitle: String, channel: String): Pair<String, String> {
    val separators = listOf(" - ", " – ", " — ", " | ")
    for (sep in separators) {
        if (rawTitle.contains(sep)) {
            val parts = rawTitle.split(sep, limit = 2)
            if (parts.size == 2) {
                val first = parts[0].trim()
                val second = parts[1].trim()
                return when {
                    channel.contains(first, ignoreCase = true) -> Pair(second, channel)
                    channel.contains(second, ignoreCase = true) -> Pair(first, channel)
                    first.length < second.length -> Pair(second, first)
                    else -> Pair(first, second)
                }
            }
        }
    }
    return Pair(rawTitle, channel)
}

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

private val BLACKLIST = listOf(
    "compilation", "compilación", "compilado", "compilations", "full album", "full-album",
    "complete album", "best of", "greatest hits", "various artists", "varios artistas",
    "playlist", "mix", "medley", "megamix", "dj set", "continuous", "hour", "hours",
    "full album", "full album stream", "album stream",
    "greatest hits", "collection", "various artists", "various-artists"
).map { it.lowercase() }
