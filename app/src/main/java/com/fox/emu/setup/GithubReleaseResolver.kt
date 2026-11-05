package com.fox.emu.setup

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * The412Banner's repos (Banners-Turnip, proton-wine) rebuild and publish a
 * new GitHub release automatically every run — sometimes multiple times a
 * day. Hardcoding a release tag/filename goes stale almost immediately, so
 * instead we hit the GitHub Releases API at setup time and pick the asset
 * whose name matches what we need out of whatever the latest release is.
 */
object GithubReleaseResolver {

    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(GhRelease::class.java)

    @JsonClass(generateAdapter = true)
    data class GhAsset(val name: String, val browser_download_url: String, val size: Long = 0)

    @JsonClass(generateAdapter = true)
    data class GhRelease(val tag_name: String, val assets: List<GhAsset>)

    class NoMatchingAssetException(msg: String) : Exception(msg)

    /**
     * @param repo "owner/name"
     * @param mustContain all of these substrings must appear in the asset filename (case-insensitive)
     * @param mustNotContain none of these may appear
     */
    fun resolveLatestAsset(
        repo: String,
        mustContain: List<String>,
        mustNotContain: List<String> = emptyList()
    ): GhAsset {
        val req = Request.Builder()
            .url("https://api.github.com/repos/$repo/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw Exception("GitHub API ${resp.code} for $repo — rate limited or repo moved")
            }
            val body = resp.body?.string() ?: throw Exception("Empty release body for $repo")
            val release = adapter.fromJson(body) ?: throw Exception("Bad JSON from $repo")

            val match = release.assets.firstOrNull { asset ->
                val n = asset.name.lowercase()
                mustContain.all { n.contains(it.lowercase()) } &&
                        mustNotContain.none { n.contains(it.lowercase()) }
            }

            return match ?: throw NoMatchingAssetException(
                "No asset in $repo@${release.tag_name} matching +${mustContain} -${mustNotContain}. " +
                        "Available: ${release.assets.map { it.name }}"
            )
        }
    }
}
