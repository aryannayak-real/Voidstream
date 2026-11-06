package com.voidstream.app.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

private val ReleaseJson = Json { ignoreUnknownKeys = true }
private const val SHA256_PREFIX = "sha256:"

@Serializable
internal data class GitHubRelease(
    @SerialName("tag_name") val tagName: String = "",
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
internal data class GitHubAsset(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
    val size: Long = 0,
    val digest: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

internal fun parseGitHubRelease(json: String): GitHubRelease = ReleaseJson.decodeFromString(GitHubRelease.serializer(), json)

/** [release] as an [AppRelease] when it is newer than [currentVersion], otherwise null. */
internal fun GitHubRelease.toAppReleaseIfNewer(
    currentVersion: String,
    supportedAbis: List<String>,
): AppRelease? {
    if (tagName.isBlank() || !AppVersions.isNewer(tagName, currentVersion)) return null
    return AppRelease(
        version = AppVersions.normalize(tagName),
        tag = tagName,
        notes = body.orEmpty(),
        publishedAt = publishedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
        pageUrl = htmlUrl,
        apk = selectApk(assets)?.toReleaseApk(),
    )
}

internal fun selectApk(assets: List<GitHubAsset>): GitHubAsset? =
    assets.firstOrNull { it.name.equals("app-release.apk", ignoreCase = true) }

private fun GitHubAsset.toReleaseApk(): ReleaseApk =
    ReleaseApk(
        name = name,
        url = downloadUrl,
        sizeBytes = size,
        sha256 = digest?.takeIf { it.startsWith(SHA256_PREFIX) }?.removePrefix(SHA256_PREFIX)?.lowercase(),
    )
