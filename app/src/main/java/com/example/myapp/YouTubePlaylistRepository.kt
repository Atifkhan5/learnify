package com.example.myapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object YouTubePlaylistRepository {

    private val youtubeApiKey = BuildConfig.YOUTUBE_API_KEY
    private val playlistIdRegex = Regex("[?&]list=([a-zA-Z0-9_-]+)")

    fun isPlaylistLink(url: String): Boolean {
        return playlistIdRegex.containsMatchIn(url)
    }

    fun extractPlaylistId(url: String): String? {
        return playlistIdRegex.find(url)?.groupValues?.get(1)
    }

    // Returns a list of (title, videoId) pairs, one per video in the playlist,
    // in playlist order. Follows pagination until the whole playlist is read.
    suspend fun fetchPlaylistVideos(playlistId: String): List<Pair<String, String>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Pair<String, String>>()
            var pageToken: String? = null
            val encodedPlaylistId = URLEncoder.encode(playlistId, "UTF-8")

            do {
                val pageTokenParam = if (pageToken != null) "&pageToken=$pageToken" else ""
                val urlString =
                    "https://www.googleapis.com/youtube/v3/playlistItems" +
                            "?part=snippet" +
                            "&maxResults=50" +
                            "&playlistId=$encodedPlaylistId" +
                            "&key=$YOUTUBE_API_KEY" +
                            pageTokenParam

                val connection = URL(urlString).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode != 200) {
                    val errorBody = connection.errorStream?.bufferedReader()?.readText()
                    throw Exception(
                        "YouTube API error (${connection.responseCode}): ${errorBody ?: "unknown"}"
                    )
                }

                val responseText = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(responseText)
                val items = json.optJSONArray("items")

                if (items != null) {
                    for (i in 0 until items.length()) {
                        val snippet = items.getJSONObject(i).optJSONObject("snippet")
                        val title = snippet?.optString("title") ?: "Untitled"
                        val videoId = snippet
                            ?.optJSONObject("resourceId")
                            ?.optString("videoId")

                        if (!videoId.isNullOrBlank()) {
                            results.add(title to videoId)
                        }
                    }
                }

                pageToken = json.optString("nextPageToken").ifBlank { null }

                // Safety cap so a huge or misbehaving playlist can't loop forever.
            } while (pageToken != null && results.size < 200)

            results
        }
    }
}