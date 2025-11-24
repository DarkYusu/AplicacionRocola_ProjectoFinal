package com.aplicaciones_android.aplicacionrocola_projectofinal.data.remote.model

import org.json.JSONObject

data class YoutubeSearchResponse(
    val nextPageToken: String?,
    val videoIds: List<String>,
    val rawItems: List<JSONObject>
) {
    companion object {
        fun fromJson(json: JSONObject): YoutubeSearchResponse {
            val next = json.optString("nextPageToken", null)
            val items = json.optJSONArray("items")
            val ids = mutableListOf<String>()
            val raw = mutableListOf<JSONObject>()
            if (items != null) {
                for (i in 0 until items.length()) {
                    val entry = items.getJSONObject(i)
                    val idObj = entry.optJSONObject("id")
                    val videoId = idObj?.optString("videoId")
                    if (!videoId.isNullOrBlank()) {
                        ids.add(videoId)
                        raw.add(entry)
                    }
                }
            }
            return YoutubeSearchResponse(next, ids, raw)
        }
    }
}

data class YoutubeVideosResponse(
    val categoryById: Map<String, String>,
    val durationById: Map<String, Long>
) {
    companion object {
        fun fromJson(json: JSONObject): YoutubeVideosResponse {
            val items = json.optJSONArray("items")
            val categories = mutableMapOf<String, String>()
            val durations = mutableMapOf<String, Long>()
            if (items != null) {
                for (i in 0 until items.length()) {
                    val video = items.getJSONObject(i)
                    val id = video.optString("id")
                    val snippet = video.optJSONObject("snippet")
                    val content = video.optJSONObject("contentDetails")
                    val category = snippet?.optString("categoryId") ?: ""
                    val durationIso = content?.optString("duration")
                    if (id.isNotBlank()) {
                        categories[id] = category
                        durations[id] = durationIso.toDurationSeconds()
                    }
                }
            }
            return YoutubeVideosResponse(categories, durations)
        }

        private fun String?.toDurationSeconds(): Long {
            if (this.isNullOrBlank()) return 0L
            val matcher = PATTERN.matcher(this)
            if (!matcher.matches()) return 0L
            var hours = 0L
            var minutes = 0L
            var seconds = 0L
            matcher.group(1)?.toLongOrNull()?.let { hours = it }
            matcher.group(2)?.toLongOrNull()?.let { minutes = it }
            matcher.group(3)?.toLongOrNull()?.let { seconds = it }
            return hours * 3600 + minutes * 60 + seconds
        }

        private val PATTERN = java.util.regex.Pattern.compile("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")
    }
}

