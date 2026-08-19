import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asJsoup
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class Rule34Video : ParsedAnimeHttpSource() {
    override val name = "Rule34Video"
    override val baseUrl = "https://rule34video.com"
    override val lang = "en"
    override val supportsLatest = true

    private val sourceHeaders: Headers by lazy {
        headersBuilder()
            .set("User-Agent", USER_AGENT)
            .set("Referer", "$baseUrl/")
            .set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()
    }

    override fun headersBuilder() = super.headersBuilder()
        .add("User-Agent", USER_AGENT)
        .add("Referer", "$baseUrl/")
        .add("Origin", baseUrl)

    override fun popularAnimeRequest(page: Int): Request =
        GET(listUrl(page, "post_date", "desc"), sourceHeaders)

    override fun popularAnimeSelector() = VIDEO_CARD_SELECTOR
    override fun popularAnimeFromElement(element: Element) = animeFromElement(element)
    override fun popularAnimeNextPageSelector() = NEXT_PAGE_SELECTOR

    override fun latestUpdatesRequest(page: Int): Request =
        GET(listUrl(page, "post_date", "desc"), sourceHeaders)

    override fun latestUpdatesSelector() = VIDEO_CARD_SELECTOR
    override fun latestUpdatesFromElement(element: Element) = animeFromElement(element)
    override fun latestUpdatesNextPageSelector() = NEXT_PAGE_SELECTOR

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val params = filterParameters(filters).toMutableMap()
        if (query.isNotBlank()) params["q"] = query.trim()
        if (page > 1) params["page"] = page.toString()
        return GET(buildUrl("/search/", params), sourceHeaders)
    }

    override fun searchAnimeSelector() = VIDEO_CARD_SELECTOR
    override fun searchAnimeFromElement(element: Element) = animeFromElement(element)
    override fun searchAnimeNextPageSelector() = NEXT_PAGE_SELECTOR

    override fun getFilterList() = AnimeFilterList(
        AnimeFilter.Header("Rule34Video filters; IDs may be separated by commas"),
        SortFilter(),
        DateFilter(),
        ModelIdsFilter(),
        CategoryIdsFilter(),
        TagIdsFilter(),
        UploaderFilter(),
        MinimumDurationFilter(),
        MaximumDurationFilter(),
        BlacklistFilter(),
    )

    override fun animeDetailsRequest(anime: SAnime): Request {
        val popup = popupUrl(anime.url.toAbsoluteUrl())
        return GET(popup, sourceHeaders.newBuilder().set("Referer", anime.url.toAbsoluteUrl()).build())
    }

    override fun animeDetailsParse(document: Document): SAnime = parseDetails(document)

    override fun episodeListParse(response: Response): List<SEpisode> {
        val document = response.asJsoup()
        val pageUrl = response.request.url.toString()
        return listOf(
            SEpisode.create().apply {
                setUrlWithoutDomain(pageUrl)
                name = document.selectFirst("h1.title_video, h1")?.text()?.trim().orEmpty().ifBlank { "Video" }
                episode_number = 1f
                date_upload = 0L
            },
        )
    }

    override fun videoListRequest(episode: SEpisode): Request {
        val popup = popupUrl(episode.url.toAbsoluteUrl())
        return GET(popup, sourceHeaders.newBuilder().set("Referer", episode.url.toAbsoluteUrl()).build())
    }

    override fun videoListParse(response: Response): List<Video> {
        val document = response.asJsoup()
        val links = document.select("a.tag_item.tag_item_download[href*='/get_file/']")
            .mapNotNull { element ->
                val url = element.absUrl("href").ifBlank { element.attr("href").toAbsoluteUrl() }
                if (url.isBlank()) return@mapNotNull null
                val quality = element.text().trim().ifBlank { qualityFromUrl(url) }
                Video(url, quality, url, mediaHeaders(response))
            }
            .distinctBy { it.videoUrl }

        if (links.isNotEmpty()) return links

        val fallback = document.select("source[src], video[src], a[href$='.mp4'], a[href*='.m3u8']")
            .mapNotNull { element ->
                val url = element.absUrl("src").ifBlank {
                    element.absUrl("href").ifBlank { element.attr("src").toAbsoluteUrl() }
                }
                url.takeIf { it.isNotBlank() }?.let { Video(it, qualityFromUrl(it), it, mediaHeaders(response)) }
            }
            .distinctBy { it.videoUrl }
        return fallback
    }

    override fun videoUrlParse(response: Response): String = response.request.url.toString()

    private fun mediaHeaders(response: Response): Headers {
        val cookies = response.headers.values("Set-Cookie")
            .map { it.substringBefore(';').trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("; ")
        return sourceHeaders.newBuilder()
            .set("Referer", response.request.url.toString())
            .apply { if (cookies.isNotBlank()) set("Cookie", cookies) }
            .build()
    }

    private fun parseDetails(document: Document): SAnime = SAnime.create().apply {
        val title = document.selectFirst("h1.title_video, h1")?.text()?.trim().orEmpty()
        this.title = title
        thumbnail_url = document.selectFirst("meta[property='og:image']")?.attr("content")?.toAbsoluteUrl()
            ?: document.selectFirst("img[src], img[data-original]")?.let {
                it.attr("data-original").ifBlank { it.attr("src") }.toAbsoluteUrl()
            }

        val categories = document.select("a.item.btn_link.video_meta_pill[href*='/categories/']")
            .eachText().cleanNames()
        val artists = document.select("a.item.btn_link.video_meta_pill[href*='/models/']")
            .eachText().cleanNames()
        val uploaders = document.select("a[href*='/members/']").eachText().cleanNames()
        val tags = document.select("a.tag_item[href*='/tags/']").eachText().cleanNames()
        val freeDescription = document.selectFirst("#tab_video_info > .row .label em")?.wholeText()?.trim().orEmpty()
        val originalPage = document.selectFirst("a[href*='/video/']")?.absUrl("href").orEmpty().ifBlank { document.baseUri() }
        val downloadQualities = document.select("a.tag_item.tag_item_download[href*='/get_file/']")
            .eachText().cleanNames()

        author = artists.joinToString(", ").ifBlank { uploaders.joinToString(", ") }
        artist = author
        genre = (categories + tags).distinct().joinToString(", ").ifBlank { null }
        description = buildString {
            if (freeDescription.isNotBlank()) appendLine(freeDescription)
            appendLine("Original page: $originalPage")
            if (categories.isNotEmpty()) appendLine("Categories: ${categories.joinToString(", ")}")
            if (artists.isNotEmpty()) appendLine("Artist / model: ${artists.joinToString(", ")}")
            if (uploaders.isNotEmpty()) appendLine("Uploaded by: ${uploaders.joinToString(", ")}")
            if (tags.isNotEmpty()) appendLine("Tags: ${tags.joinToString(", ")}")
            if (downloadQualities.isNotEmpty()) appendLine("Available qualities: ${downloadQualities.joinToString(", ")}")
        }.trim()
        status = SAnime.UNKNOWN
        initialized = true
    }

    private fun animeFromElement(element: Element): SAnime {
        val link = element.selectFirst("a.th.js-open-popup[href*='/video/'], a[href*='/video/']") ?: element
        val detailUrl = link.absUrl("href").ifBlank { link.attr("href").toAbsoluteUrl() }
        return SAnime.create().apply {
            setUrlWithoutDomain(popupUrl(detailUrl))
            title = link.attr("title").trim().ifBlank {
                element.selectFirst(".thumb_title")?.text()?.trim() ?: link.text().trim()
            }
            thumbnail_url = element.selectFirst("img[data-original], img[data-webp], img[src]")?.let {
                it.attr("data-original").ifBlank { it.attr("data-webp") }.ifBlank { it.attr("src") }.toAbsoluteUrl()
            }
        }
    }

    private fun popupUrl(url: String): String {
        val id = VIDEO_ID_REGEX.find(url)?.groupValues?.getOrNull(1) ?: return url
        return "$baseUrl/popup-video/$id/?popup_id=1"
    }

    private fun listUrl(page: Int, sortBy: String, sortDir: String): String {
        val params = linkedMapOf<String, String>()
        params["sort_by"] = sortBy
        params["sort_dir"] = sortDir
        if (page > 1) params["page"] = page.toString()
        return buildUrl("/", params)
    }

    private fun filterParameters(filters: AnimeFilterList): Map<String, String> {
        val params = linkedMapOf<String, String>()
        val sort = filters.filterIsInstance<SortFilter>().firstOrNull()?.state ?: 0
        when (sort) {
            1 -> { params["sort_by"] = "video_viewed"; params["sort_dir"] = "desc" }
            2 -> { params["sort_by"] = "rating"; params["sort_dir"] = "desc" }
            else -> { params["sort_by"] = "post_date"; params["sort_dir"] = "desc" }
        }
        val dateState = filters.filterIsInstance<DateFilter>().firstOrNull()?.state ?: 0
        if (dateState > 0) params["date"] = arrayOf("", "today", "week", "month").getOrElse(dateState) { "" }
        filters.filterIsInstance<ModelIdsFilter>().firstOrNull()?.state?.trim()?.takeIf { it.isNotBlank() }?.let { params["model_ids"] = it }
        filters.filterIsInstance<CategoryIdsFilter>().firstOrNull()?.state?.trim()?.takeIf { it.isNotBlank() }?.let { params["category_ids"] = it }
        filters.filterIsInstance<TagIdsFilter>().firstOrNull()?.state?.trim()?.takeIf { it.isNotBlank() }?.let { params["tag_ids"] = it }
        filters.filterIsInstance<UploaderFilter>().firstOrNull()?.state?.trim()?.takeIf { it.isNotBlank() }?.let { params["uploader"] = it }
        filters.filterIsInstance<MinimumDurationFilter>().firstOrNull()?.state?.trim()?.takeIf { it.isNotBlank() }?.let { params["min_duration"] = it }
        filters.filterIsInstance<MaximumDurationFilter>().firstOrNull()?.state?.trim()?.takeIf { it.isNotBlank() }?.let { params["max_duration"] = it }
        filters.filterIsInstance<BlacklistFilter>().firstOrNull()?.state?.trim()?.takeIf { it.isNotBlank() }?.let { params["blacklist"] = it }
        return params
    }

    private fun buildUrl(path: String, parameters: Map<String, String>): String {
        val query = parameters.filterValues { it.isNotBlank() }
            .entries.joinToString("&") {
                "${encode(it.key)}=${encode(it.value)}"
            }
        return "$baseUrl$path" + if (query.isBlank()) "" else "?$query"
    }

    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun qualityFromUrl(url: String): String =
        Regex("(?i)(1080p|720p|480p|360p|mp4|m3u8)").find(url)?.value ?: "Video"

    private fun List<String>.cleanNames() = map { it.trim() }.filter { it.isNotBlank() }.distinct()

    private fun String.toAbsoluteUrl(): String = when {
        isBlank() -> this
        startsWith("http://") || startsWith("https://") -> this
        startsWith("//") -> "https:$this"
        startsWith("/") -> baseUrl + this
        else -> "$baseUrl/$this"
    }

    private class SortFilter : AnimeFilter.Select<String>(
        "Sort",
        arrayOf("Newest", "Most viewed", "Top rated"),
    )

    private class DateFilter : AnimeFilter.Select<String>(
        "Date added",
        arrayOf("Any time", "Today", "This week", "This month"),
    )

    private class ModelIdsFilter : AnimeFilter.Text("Artist/model IDs")
    private class CategoryIdsFilter : AnimeFilter.Text("Category IDs")
    private class TagIdsFilter : AnimeFilter.Text("Tag IDs")
    private class UploaderFilter : AnimeFilter.Text("Uploader")
    private class MinimumDurationFilter : AnimeFilter.Text("Minimum duration (seconds)")
    private class MaximumDurationFilter : AnimeFilter.Text("Maximum duration (seconds)")
    private class BlacklistFilter : AnimeFilter.Text("Blacklist IDs")

    private companion object {
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/131 Mobile Safari/537.36"
        const val VIDEO_CARD_SELECTOR = "div.item.thumb a.th.js-open-popup[href*='/video/']"
        const val NEXT_PAGE_SELECTOR = "a[rel=next], .pagination a.next, a.next"
        val VIDEO_ID_REGEX = Regex("/video/(\\d+)(?:/|$)")
    }
}
