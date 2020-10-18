package ru.avdim.github

import com.sample.getGithubRepoReleases
import io.ktor.client.*
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TestReleases {
    @Test
    fun testListDownloads() {
        runBlocking {
            val client = HttpClient()
            val releases = client.getGithubRepoReleases("avdim", "unicorn")
            val downloadUrls = releases.flatMap { it.assets }.map { it.browser_download_url }
            println(downloadUrls.joinToString("\n"))
        }
    }
}
