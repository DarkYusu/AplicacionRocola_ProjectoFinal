package com.aplicaciones_android.aplicacionrocola_projectofinal.data.remote

import com.aplicaciones_android.aplicacionrocola_projectofinal.data.remote.model.YoutubeSearchResponse
import com.aplicaciones_android.aplicacionrocola_projectofinal.data.remote.model.YoutubeVideosResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class YoutubeRemoteDataSource(
    private val apiKey: String,
    private val client: OkHttpClient = OkHttpClient()
) {

    suspend fun searchVideos(query: String, pageToken: String?): YoutubeSearchResponse {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val tokenParam = if (pageToken.isNullOrBlank()) "" else "&pageToken=$pageToken"
        val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=15&q=$encoded$tokenParam&key=$apiKey"
        val response = performRequest(url)
        return YoutubeSearchResponse.fromJson(JSONObject(response))
    }

    suspend fun fetchVideosDetails(ids: List<String>): YoutubeVideosResponse {
        val idsParam = ids.joinToString(",")
        val url = "https://www.googleapis.com/youtube/v3/videos?part=snippet,contentDetails&id=$idsParam&key=$apiKey"
        val response = performRequest(url)
        return YoutubeVideosResponse.fromJson(JSONObject(response))
    }

    private fun performRequest(url: String): String {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}")
            }
            return response.body?.string() ?: ""
        }
    }
}
