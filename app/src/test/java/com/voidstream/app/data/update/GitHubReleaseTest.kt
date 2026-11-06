package com.voidstream.app.data.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class GitHubReleaseTest {
    private fun asset(name: String, size: Long = 1, digest: String? = null) =
        GitHubAsset(name, "https://example.test/$name", size, digest)

    @Test
    fun `selects the single universal release apk`() {
        val assets = listOf(asset("checksums.txt"), asset("other.apk"), asset("app-release.apk"))
        assertThat(selectApk(assets)?.name).isEqualTo("app-release.apk")
    }

    @Test
    fun `returns no apk when the universal release apk is missing`() {
        assertThat(selectApk(listOf(asset("other.apk")))).isNull()
    }

    @Test
    fun `versions compare numerically and ignore prefix and suffix`() {
        assertThat(AppVersions.isNewer("v2.10.0", "2.9.9")).isTrue()
        assertThat(AppVersions.isNewer("v2.2.5", "2.2.5-debug")).isFalse()
        assertThat(AppVersions.isNewer("2.2", "2.2.0")).isFalse()
        assertThat(AppVersions.isNewer("v2.2.1", "2.3.0")).isFalse()
    }

    @Test
    fun `a newer stable release carries apk size digest and date`() {
        val json = "{\"tag_name\":\"v2.3.0\",\"body\":\"## New features\",\"html_url\":\"https://github.com/aryannayak-real/Voidstream/releases/tag/v2.3.0\",\"published_at\":\"2026-09-28T10:00:00Z\",\"assets\":[{\"name\":\"app-release.apk\",\"browser_download_url\":\"https://example.test/a\",\"size\":17613193,\"digest\":\"sha256:F900AB\"}]}"
        val release = parseGitHubRelease(json).toAppReleaseIfNewer("2.2.5", emptyList())
        assertThat(release?.version).isEqualTo("2.3.0")
        assertThat(release?.publishedAt).isEqualTo(Instant.parse("2026-09-28T10:00:00Z"))
        assertThat(release?.apk).isEqualTo(ReleaseApk("app-release.apk", "https://example.test/a", 17613193, "f900ab"))
    }

    @Test
    fun `the running version is not an update`() {
        val release = GitHubRelease(tagName = "v2.2.5").toAppReleaseIfNewer("2.2.5", emptyList())
        assertThat(release).isNull()
    }

    @Test
    fun `the update page displays the stable version`() {
        assertThat(AppVersions.installedLabel("3.0.0")).isEqualTo("3.0.0")
    }
}
