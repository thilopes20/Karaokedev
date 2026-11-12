package com.karaokedev.app.library

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class Song(
    val id: String = "",
    val title: String = "",
    val audio_url: String = "",
    val lyrics_url: String? = null
)

interface MusicSource {
    suspend fun listSongs(): List<Song>
}

class SupabaseMusicSource(
    private val supabaseUrl: String,
    private val anonKey: String,
    private val table: String = "karaoke_songs"
) : MusicSource {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun listSongs(): List<Song> = withContext(Dispatchers.IO) {
        val conn = URL("$supabaseUrl/rest/v1/$table?select=*").openConnection() as HttpURLConnection
        try {
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $anonKey")
            conn.inputStream.bufferedReader().use { reader ->
                json.decodeFromString<List<Song>>(reader.readText())
            }
        } finally {
            conn.disconnect()
        }
    }
}
