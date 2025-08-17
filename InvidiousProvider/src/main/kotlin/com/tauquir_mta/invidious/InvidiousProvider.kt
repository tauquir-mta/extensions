package com.tauquir_mta.invidious // CORRECT: Uses UNDERSCORE

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink

class InvidiousProvider : MainAPI() {
    private val invidiousInstances = listOf(
        "https://invidious.io", "https://vid.puffyan.us",
        "https://invidious.projectsegfau.lt", "https://iv.ggtyler.dev",
        "https://invidious.kavin.rocks"
    )
    override var mainUrl = invidiousInstances.first()
    override var name = "Invidious (Tauquir)"
    override val supportedTypes = setOf(TvType.Movie)
    override var lang = "en"

    data class SearchResult(
        @JsonProperty("title") val title: String, @JsonProperty("videoId") val videoId: String,
        @JsonProperty("author") val author: String, @JsonProperty("videoThumbnails") val videoThumbnails: List<VideoThumbnail>
    )
    data class VideoThumbnail(@JsonProperty("url") val url: String)
    data class VideoDetails(
        @JsonProperty("title") val title: String, @JsonProperty("description") val description: String,
        @JsonProperty("author") val author: String
    )

    private suspend inline fun <reified T : Any> safeApiCall(apiPath: String): T? {
        for (instance in invidiousInstances) {
            try {
                val response = app.get("$instance/$apiPath")
                if (response.isSuccessful) {
                    mainUrl = instance
                    return tryParseJson(response.text)
                }
            } catch (_: Exception) {}
        }
        return null
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val response = safeApiCall<List<SearchResult>>("api/v1/search?q=$query&type=video")
        return response?.map {
            newMovieSearchResponse(it.title, it.videoId, TvType.Movie) {
                this.posterUrl = it.videoThumbnails.firstOrNull()?.url
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val videoId = url
        val details = safeApiCall<VideoDetails>("api/v1/videos/$videoId") ?: return null
        return newMovieLoadResponse(details.title, videoId, TvType.Movie, videoId) {
            this.plot = details.description
            this.actors = listOf(ActorData(Actor(details.author)))
        }
    }

    override suspend fun loadLinks(
        data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        loadExtractor("https://youtube.com/watch?v=$data", subtitleCallback, callback)
        callback(
            newExtractorLink(this.name, this.name, "$mainUrl/api/manifest/dash/id/$data") {
                this.quality = Qualities.Unknown.value
                this.type = ExtractorLinkType.DASH
            }
        )
        return true
    }
}