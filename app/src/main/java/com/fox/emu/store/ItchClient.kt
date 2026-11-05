package com.fox.emu.store

import com.fox.emu.container.Platform
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request

@JsonClass(generateAdapter = true)
data class ItchUpload(
    val id: Long,
    val filename: String,
    val p_windows: Boolean = false,
    val p_linux: Boolean = false,
    val p_android: Boolean = false,
    val size: Long = 0
)

@JsonClass(generateAdapter = true)
data class ItchGame(
    val id: Long,
    val title: String,
    val url: String
)

@JsonClass(generateAdapter = true)
data class OwnedKey(val game: ItchGame, val download_key_id: Long)

@JsonClass(generateAdapter = true)
data class OwnedKeysResponse(val owned_keys: List<OwnedKey> = emptyList())

@JsonClass(generateAdapter = true)
data class UploadsResponse(val uploads: List<ItchUpload> = emptyList())

class ItchClient(private val apiKey: String) {
    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val base = "https://itch.io/api/1/$apiKey"

    fun fetchOwnedGames(): List<OwnedKey> {
        val req = Request.Builder().url("$base/my-owned-keys").build()
        val body = client.newCall(req).execute().body?.string() ?: return emptyList()
        return moshi.adapter(OwnedKeysResponse::class.java).fromJson(body)?.owned_keys ?: emptyList()
    }

    fun fetchUploads(gameId: Long, downloadKey: String? = null): List<ItchUpload> {
        val url = if (downloadKey != null)
            "$base/download-key/$downloadKey/uploads"
        else
            "$base/game/$gameId/uploads"
        val req = Request.Builder().url(url).build()
        val body = client.newCall(req).execute().body?.string() ?: return emptyList()
        return moshi.adapter(UploadsResponse::class.java).fromJson(body)?.uploads ?: emptyList()
    }

    fun uploadsForPlatform(uploads: List<ItchUpload>, platform: Platform) = uploads.filter {
        when (platform) {
            Platform.WINDOWS -> it.p_windows
            Platform.LINUX -> it.p_linux
            Platform.ANDROID -> it.p_android
        }
    }

    fun downloadUrl(uploadId: Long, downloadKey: String) =
        "$base/download-key/$downloadKey/download/$uploadId"
}
