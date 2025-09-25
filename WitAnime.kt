package eu.kanade.tachiyomi.animeextension.ar.witanime

import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class WitAnime : ParsedAnimeHttpSource(), ConfigurableAnimeSource {

    override val name = "WitAnime"
    override val baseUrl = "https://witanime.com"
    override val lang = "ar"
    override val supportsLatest = true

    // ============================ Popular ============================
    override fun popularAnimeRequest(page: Int): Request =
        GET("$baseUrl/anime-list/page/$page/")

    override fun popularAnimeSelector(): String = "div.anime-card"

    override fun popularAnimeFromElement(element: Element): SAnime {
        val anime = SAnime.create()
        anime.setUrlWithoutDomain(element.selectFirst("a")!!.attr("href"))
        anime.title = element.selectFirst("h3.anime-title")?.text().orEmpty()
        anime.thumbnail_url = element.selectFirst("img")?.attr("src")
        return anime
    }

    override fun popularAnimeNextPageSelector(): String = "a.next.page-numbers"

    // ============================ Episodes ============================
    override fun episodeListSelector(): String = "ul.episodes-list li"

    override fun episodeFromElement(element: Element): SEpisode {
        val ep = SEpisode.create()
        ep.setUrlWithoutDomain(element.selectFirst("a")!!.attr("href"))
        ep.name = element.selectFirst("a")?.text().orEmpty()
        return ep
    }

    // ============================ Video Links ============================
    override fun videoListParse(response: okhttp3.Response): List<Video> {
        val document = response.asJsoup()
        val videos = mutableListOf<Video>()
        val iframe = document.selectFirst("iframe")?.attr("src")
        if (iframe != null) {
            videos.add(Video(iframe, "Default", iframe))
        }
        return videos
    }

    override fun videoListSelector(): String = ""

    override fun videoFromElement(element: Element): Video = throw UnsupportedOperationException()

    override fun videoUrlParse(document: Document): String = throw UnsupportedOperationException()

    // ============================ Latest ============================
    override fun latestUpdatesRequest(page: Int): Request =
        GET("$baseUrl/new-episodes/page/$page/")

    override fun latestUpdatesSelector(): String = "div.anime-card"

    override fun latestUpdatesFromElement(element: Element): SAnime = popularAnimeFromElement(element)

    override fun latestUpdatesNextPageSelector(): String = "a.next.page-numbers"

    // ============================ Search ============================
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request =
        GET("$baseUrl/?s=$query")

    override fun searchAnimeSelector(): String = "div.anime-card"

    override fun searchAnimeFromElement(element: Element): SAnime = popularAnimeFromElement(element)

    override fun searchAnimeNextPageSelector(): String = "a.next.page-numbers"
}
