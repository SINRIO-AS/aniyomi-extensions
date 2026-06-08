package eu.kanade.tachiyomi.animeextension.ar.shahiidanime

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asJsoup
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class Shahiidanime : ParsedAnimeHttpSource() {
    override val name = "Shahiid Anime"
    override val baseUrl = "https://shahiid-anime.net"
    override val lang = "ar"
    override val supportsLatest = true

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US)

    override fun headersBuilder() = super.headersBuilder()
        .add("Referer", baseUrl)
        .add("User-Agent", "Mozilla/5.0 (Linux; Android 13) Aniyomi")

    override fun popularAnimeRequest(page: Int): Request = GET(archiveUrl("series", page), headers)
    override fun popularAnimeSelector() = "div.one-poster"
    override fun popularAnimeFromElement(element: Element) = animeFromElement(element)
    override fun popularAnimeNextPageSelector() = "link[rel=next]"

    override fun latestUpdatesRequest(page: Int): Request = GET(archiveUrl("episodes", page), headers)
    override fun latestUpdatesSelector() = popularAnimeSelector()
    override fun latestUpdatesFromElement(element: Element) = animeFromElement(element)
    override fun latestUpdatesNextPageSelector() = popularAnimeNextPageSelector()

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        if (query.isBlank()) return popularAnimeRequest(page)
        val q = query.trim().replace(" ", "+")
        val url = if (page == 1) "$baseUrl/?s=$q" else "$baseUrl/page/$page/?s=$q"
        return GET(url, headers)
    }
    override fun searchAnimeSelector() = popularAnimeSelector()
    override fun searchAnimeFromElement(element: Element) = animeFromElement(element)
    override fun searchAnimeNextPageSelector() = popularAnimeNextPageSelector()

    override fun animeDetailsParse(document: Document): SAnime = SAnime.create().apply {
        title = document.selectFirst("h1")?.text()?.trim().orEmpty()
        thumbnail_url = document.selectFirst("meta[property=og:image]")?.attr("content")
            ?: document.selectFirst("img.wp-post-image")?.absUrl("src")
        description = document.selectFirst("meta[property=og:description]")?.attr("content")
            ?: document.selectFirst("div.entry-content, div.page-cntn")?.text()
        genre = document.select("a[rel=tag], a[rel='category tag']").eachText().distinct().joinToString().ifBlank { null }
        status = when {
            document.text().contains("مكتمل") -> SAnime.COMPLETED
            document.text().contains("مستمر") -> SAnime.ONGOING
            else -> SAnime.UNKNOWN
        }
        initialized = true
    }

    override fun episodeListSelector() = "#EpsList a[href*=/episodes/], a[href*=/episodes/][title*=الحلقة]"
    override fun episodeFromElement(element: Element): SEpisode = SEpisode.create().apply {
        setUrlWithoutDomain(element.absUrl("href"))
        name = element.text().trim().ifBlank { element.attr("title") }
        episode_number = Regex("(?:الحلقة|ep)[^0-9]*(\\d+)", RegexOption.IGNORE_CASE)
            .find(name)?.groupValues?.get(1)?.toFloatOrNull() ?: -1f
    }

    override fun episodeListParse(response: Response): List<SEpisode> {
        val document = response.asJsoup()
        val episodes = document.select(episodeListSelector()).map(::episodeFromElement).distinctBy { it.url }
        if (episodes.isNotEmpty()) return episodes.reversed()
        return listOf(SEpisode.create().apply {
            setUrlWithoutDomain(document.location())
            name = document.selectFirst("h1")?.text()?.trim().orEmpty().ifBlank { "Episode" }
            date_upload = parseDate(document.selectFirst("meta[property=article:modified_time]")?.attr("content"))
            episode_number = Regex("(?:الحلقة|ep)[^0-9]*(\\d+)", RegexOption.IGNORE_CASE)
                .find(name)?.groupValues?.get(1)?.toFloatOrNull() ?: 1f
        })
    }

    override fun hosterListParse(response: Response): List<Hoster> {
        val document = response.asJsoup()
        val hosters = document.select("a.buttosn[data-frameserver]").mapNotNull { el ->
            val frame = el.attr("data-frameserver").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val name = el.text().trim().ifBlank { "Embed" }
            val url = when (name.lowercase()) {
                "dood" -> "https://dood.yt/e/$frame"
                else -> "https://share4max.com/iframe/$frame"
            }
            Hoster(hosterUrl = url, hosterName = name, videoList = listOf(Video(url, name, url, headers)))
        }
        if (hosters.isNotEmpty()) return hosters
        val iframe = document.selectFirst("iframe[src]")?.absUrl("src") ?: return emptyList()
        return listOf(Hoster(hosterUrl = iframe, hosterName = "Embed", videoList = listOf(Video(iframe, "Embed", iframe, headers))))
    }

    override fun videoListParse(response: Response, hoster: Hoster): List<Video> {
        hoster.videoList?.let { return it }
        val url = response.request.url.toString()
        return listOf(Video(url, hoster.hosterName.ifBlank { "Embed" }, url, headers))
    }

    override fun videoUrlParse(response: Response): String = response.request.url.toString()

    override fun seasonListSelector() = popularAnimeSelector()
    override fun seasonFromElement(element: Element) = animeFromElement(element)

    private fun animeFromElement(element: Element): SAnime = SAnime.create().apply {
        val a = element.selectFirst("h2 a, a[href]")!!
        setUrlWithoutDomain(a.absUrl("href"))
        title = element.selectFirst("h2")?.text()?.trim() ?: a.text().trim()
        thumbnail_url = element.selectFirst("img")?.absUrl("src")
    }

    private fun archiveUrl(path: String, page: Int) = if (page == 1) "$baseUrl/$path/" else "$baseUrl/$path/page/$page/"

    private fun parseDate(date: String?): Long = runCatching {
        if (date.isNullOrBlank()) 0L else dateFormat.parse(date)?.time ?: 0L
    }.getOrDefault(0L)
}
