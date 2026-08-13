package eu.kanade.tachiyomi.animeextension.ar.hentairisto

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.lib.streamtapeextractor.StreamTapeExtractor
import eu.kanade.tachiyomi.lib.voeextractor.VoeExtractor
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException

class HentaiRisto : AnimeHttpSource() {

    override val name = "Hentai Risto"

    override val baseUrl = "https://ristohentai.com"

    override val lang = "ar"

    override val supportsLatest = true

    override suspend fun getPopularAnime(page: Int): AnimesPage =
        parseAnimePage(popularAnimeRequest(page), page)

    override fun popularAnimeRequest(page: Int): Request =
        GET(
            if (page == 1) "$baseUrl/series/" else "$baseUrl/series/?offset=$page",
            headers,
        )

    override suspend fun getLatestUpdates(page: Int): AnimesPage =
        parseAnimePage(latestUpdatesRequest(page), page)

    override fun latestUpdatesRequest(page: Int): Request =
        GET(
            if (page == 1) "$baseUrl/" else "$baseUrl/page/$page/",
            headers,
        )

    override suspend fun getSearchAnime(
        page: Int,
        query: String,
        filters: AnimeFilterList,
    ): AnimesPage {
        if (query.isBlank()) return getPopularAnime(page)

        return parseAnimePage(searchAnimeRequest(page, query, filters), page)
    }

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val path = if (page == 1) "$baseUrl/" else "$baseUrl/page/$page/"
        val url = path.toHttpUrl().newBuilder()
            .addQueryParameter("s", query)
            .build()

        return GET(url, headers)
    }

    override suspend fun getAnimeDetails(anime: SAnime): SAnime {
        val document = fetchDocument(absoluteUrl(anime.url))

        return SAnime.create().apply {
            url = anime.url
            title = document.selectFirst("h1.PostTitle, h1")?.text()?.trim()
                ?.takeIf(String::isNotBlank)
                ?: anime.title
            thumbnail_url = document.coverUrl().ifBlank { anime.thumbnail_url }
            description = document.descriptionText()
            genre = document.select("a[href*='/genre/']")
                .map(Element::text)
                .filter(String::isNotBlank)
                .distinct()
                .joinToString(", ")
            status = if (document.select("a[href*='/complated-series/']").isNotEmpty()) {
                SAnime.COMPLETED
            } else {
                SAnime.UNKNOWN
            }
            initialized = true
        }
    }

    override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> {
        val episodes = fetchDocument(absoluteUrl(anime.url))
            .select(".EpisodesList a[href]")
            .mapIndexed { index, element ->
                val name = element.text().trim().ifBlank { "Episode ${index + 1}" }
                SEpisode.create().apply {
                    this.name = name
                    url = relativeUrl(element.absUrl("href"))
                    episode_number = EPISODE_NUMBER_REGEX.find(name)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.toFloatOrNull()
                        ?: (index + 1).toFloat()
                    date_upload = 0L
                }
            }

        return episodes.sortedBy { it.episode_number }
    }

    override suspend fun getVideoList(episode: SEpisode): List<Video> {
        val watchUrl = absoluteUrl(episode.url).trimEnd('/') + "/watch"
        val document = fetchDocument(watchUrl)

        val videos = document.select("#watch li[data-watch]").flatMap { server ->
            val hostUrl = server.attr("data-watch").trim()
            if (hostUrl.isBlank()) {
                emptyList()
            } else {
                extractPublicHost(hostUrl)
            }
        }

        return videos
            .filter { it.videoUrl.isNotBlank() }
            .distinctBy { it.videoUrl }
            .sortedByDescending { it.videoTitle.contains("1080") }
    }

    private fun parseAnimePage(request: Request, page: Int): AnimesPage {
        val document = executeRequest(request)
        val animeList = document.select(".MovieItem")
            .mapNotNull(::animeFromElement)
            .distinctBy { it.url }

        return AnimesPage(animeList, document.hasNextPage(page))
    }

    private fun animeFromElement(element: Element): SAnime? {
        val anchor = element.selectFirst("a[href]") ?: return null
        val url = anchor.absUrl("href")
        if (url.isBlank()) return null

        val title = element.selectFirst(".title, h1, h2, h3, h4")?.text()?.trim()
            ?.takeIf(String::isNotBlank)
            ?: anchor.attr("title").trim().takeIf(String::isNotBlank)
            ?: anchor.text().trim()

        if (title.isBlank()) return null

        return SAnime.create().apply {
            this.title = title
            this.url = relativeUrl(url)
            thumbnail_url = element.selectFirst("img[src]")?.absUrl("src")
                ?.takeIf(String::isNotBlank)
                ?: element.selectFirst(".poster, [data-style]")?.backgroundImageUrl().orEmpty()
        }
    }

    private fun extractPublicHost(hostUrl: String): List<Video> =
        try {
            when {
                hostUrl.contains("voe.sx", ignoreCase = true) -> {
                    VoeExtractor(client).videosFromUrl(hostUrl)
                }
                hostUrl.contains("streamtape.com", ignoreCase = true) -> {
                    StreamTapeExtractor(client).videosFromUrl(hostUrl)
                }
                DIRECT_MEDIA_REGEX.containsMatchIn(hostUrl) -> {
                    listOf(Video(videoUrl = hostUrl, videoTitle = "Direct"))
                }
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }

    private fun executeRequest(request: Request): Document =
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} while requesting ${request.url}")
            }
            response.asJsoup()
        }

    private fun fetchDocument(url: String): Document = executeRequest(GET(url, headers))

    private fun absoluteUrl(url: String): String =
        if (url.startsWith("http://") || url.startsWith("https://")) url else "$baseUrl$url"

    private fun relativeUrl(url: String): String =
        url.removePrefix(baseUrl).ifBlank { "/" }

    private fun Document.coverUrl(): String =
        selectFirst(".singleCover img[src], .Poster img[src], .poster img[src]")?.absUrl("src")
            ?.takeIf(String::isNotBlank)
            ?: selectFirst(".singleCover, .Poster, .poster")?.backgroundImageUrl().orEmpty()

    private fun Element.backgroundImageUrl(): String {
        val value = attr("style").ifBlank { attr("data-style") }
        return BACKGROUND_IMAGE_REGEX.find(value)?.groupValues?.getOrNull(1).orEmpty()
    }

    private fun Document.descriptionText(): String =
        selectFirst(".StoryContent, .Description, .post-content, .entry-content, article p")
            ?.text()
            ?.trim()
            .orEmpty()

    private fun Document.hasNextPage(page: Int): Boolean {
        val nextPage = page + 1
        return select("a[href]").any { anchor ->
            val href = anchor.attr("href")
            href.contains("/page/$nextPage/") ||
                href.contains("offset=$nextPage") ||
                href.contains("paged=$nextPage")
        }
    }

    private companion object {
        val EPISODE_NUMBER_REGEX = Regex(
            """(?:الحلقة|episode)\s*([0-9]+(?:\.[0-9]+)?)""",
            RegexOption.IGNORE_CASE,
        )
        val BACKGROUND_IMAGE_REGEX = Regex("""url\(['"]?([^'")]+)""")
        val DIRECT_MEDIA_REGEX = Regex("""\.(?:m3u8|mp4)(?:[?#].*)?$""", RegexOption.IGNORE_CASE)
    }
}
A
