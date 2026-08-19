from pathlib import Path
import sys

root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path('.')
path = root / 'smali_classes2/eu/kanade/tachiyomi/animeextension/en/rule34video/Rule34Video.smali'
text = path.read_text()
start = text.index('.method private final parseDetails(')
end = text.index('.end method', start) + len('.end method')
new_method = r'''.method private final parseDetails(Lokhttp3/Response;Leu/kanade/tachiyomi/animesource/model/SAnime;)Leu/kanade/tachiyomi/animesource/model/SAnime;
    .locals 10
    .param p1, "response"    # Lokhttp3/Response;
    .param p2, "original"    # Leu/kanade/tachiyomi/animesource/model/SAnime;

    invoke-virtual {p1}, Lokhttp3/Response;->body()Lokhttp3/ResponseBody;
    move-result-object v1
    invoke-virtual {v1}, Lokhttp3/ResponseBody;->string()Ljava/lang/String;
    move-result-object v1

    invoke-virtual {p1}, Lokhttp3/Response;->request()Lokhttp3/Request;
    move-result-object v2
    invoke-virtual {v2}, Lokhttp3/Request;->url()Lokhttp3/HttpUrl;
    move-result-object v2
    invoke-virtual {v2}, Lokhttp3/HttpUrl;->toString()Ljava/lang/String;
    move-result-object v2
    invoke-static {v1, v2}, Lorg/jsoup/Jsoup;->parse(Ljava/lang/String;Ljava/lang/String;)Lorg/jsoup/nodes/Document;
    move-result-object v1

    const-string v3, "h1.title_video"
    invoke-virtual {v1, v3}, Lorg/jsoup/nodes/Document;->selectFirst(Ljava/lang/String;)Lorg/jsoup/nodes/Element;
    move-result-object v3
    if-eqz v3, :title_original
    invoke-virtual {v3}, Lorg/jsoup/nodes/Element;->text()Ljava/lang/String;
    move-result-object v4
    if-eqz v4, :title_original
    invoke-interface {p2, v4}, Leu/kanade/tachiyomi/animesource/model/SAnime;->setTitle(Ljava/lang/String;)V
    goto :title_done

    :title_original
    invoke-interface {p2}, Leu/kanade/tachiyomi/animesource/model/SAnime;->getTitle()Ljava/lang/String;
    move-result-object v4
    invoke-interface {p2, v4}, Leu/kanade/tachiyomi/animesource/model/SAnime;->setTitle(Ljava/lang/String;)V

    :title_done
    const-string v3, "meta[name=description], meta[property='og:description']"
    invoke-virtual {v1, v3}, Lorg/jsoup/nodes/Document;->selectFirst(Ljava/lang/String;)Lorg/jsoup/nodes/Element;
    move-result-object v3
    if-eqz v3, :description_empty
    const-string v4, "content"
    invoke-virtual {v3, v4}, Lorg/jsoup/nodes/Element;->attr(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v4
    goto :description_ready

    :description_empty
    const-string v4, ""

    :description_ready
    const-string v3, "a[href*='/models/']"
    invoke-virtual {v1, v3}, Lorg/jsoup/nodes/Document;->select(Ljava/lang/String;)Lorg/jsoup/select/Elements;
    move-result-object v3
    invoke-virtual {v3}, Lorg/jsoup/select/Elements;->text()Ljava/lang/String;
    move-result-object v5

    const-string v3, "a[href*='/groups/'], a[href*='/albums/'], a[href*='/collections/']"
    invoke-virtual {v1, v3}, Lorg/jsoup/nodes/Document;->select(Ljava/lang/String;)Lorg/jsoup/select/Elements;
    move-result-object v3
    invoke-virtual {v3}, Lorg/jsoup/select/Elements;->text()Ljava/lang/String;
    move-result-object v6

    const-string v3, "a[href*='/tags/'], a[href*='/categories/']"
    invoke-virtual {v1, v3}, Lorg/jsoup/nodes/Document;->select(Ljava/lang/String;)Lorg/jsoup/select/Elements;
    move-result-object v3
    invoke-virtual {v3}, Lorg/jsoup/select/Elements;->text()Ljava/lang/String;
    move-result-object v7

    new-instance v8, Ljava/lang/StringBuilder;
    invoke-direct {v8}, Ljava/lang/StringBuilder;-><init>()V
    invoke-virtual {v8, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    const-string v3, "\n\nOriginal page: "
    invoke-virtual {v8, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-interface {p2}, Leu/kanade/tachiyomi/animesource/model/SAnime;->getUrl()Ljava/lang/String;
    move-result-object v3
    invoke-virtual {v8, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    const-string v3, "\nAuthor / model: "
    invoke-virtual {v8, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v8, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    const-string v3, "\nGroup / collection: "
    invoke-virtual {v8, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v8, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v4

    invoke-interface {p2, v5}, Leu/kanade/tachiyomi/animesource/model/SAnime;->setAuthor(Ljava/lang/String;)V
    invoke-interface {p2, v5}, Leu/kanade/tachiyomi/animesource/model/SAnime;->setArtist(Ljava/lang/String;)V
    invoke-interface {p2, v4}, Leu/kanade/tachiyomi/animesource/model/SAnime;->setDescription(Ljava/lang/String;)V
    invoke-interface {p2, v7}, Leu/kanade/tachiyomi/animesource/model/SAnime;->setGenre(Ljava/lang/String;)V

    const-string v3, "meta[property='og:image']"
    invoke-virtual {v1, v3}, Lorg/jsoup/nodes/Document;->selectFirst(Ljava/lang/String;)Lorg/jsoup/nodes/Element;
    move-result-object v3
    if-eqz v3, :thumbnail_original
    const-string v4, "content"
    invoke-virtual {v3, v4}, Lorg/jsoup/nodes/Element;->attr(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v4
    invoke-direct {p0, v4}, Leu/kanade/tachiyomi/animeextension/en/rule34video/Rule34Video;->absoluteUrl(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v4
    invoke-interface {p2, v4}, Leu/kanade/tachiyomi/animesource/model/SAnime;->setThumbnail_url(Ljava/lang/String;)V
    goto :thumbnail_done

    :thumbnail_original
    invoke-interface {p2}, Leu/kanade/tachiyomi/animesource/model/SAnime;->getThumbnail_url()Ljava/lang/String;
    move-result-object v4
    invoke-interface {p2, v4}, Leu/kanade/tachiyomi/animesource/model/SAnime;->setThumbnail_url(Ljava/lang/String;)V

    :thumbnail_done
    const/4 v3, 0x2
    invoke-interface {p2, v3}, Leu/kanade/tachiyomi/animesource/model/SAnime;->setStatus(I)V
    const/4 v3, 0x1
    invoke-interface {p2, v3}, Leu/kanade/tachiyomi/animesource/model/SAnime;->setInitialized(Z)V
    return-object p2
.end method'''
patched = text[:start] + new_method + text[end:]
request_start = patched.index('.method public getAnimeDetails(')
request_end = patched.index('.end method', request_start) + len('.end method')
new_request = r'''.method public getAnimeDetails(Leu/kanade/tachiyomi/animesource/model/SAnime;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    .locals 8
    .param p1, "anime"    # Leu/kanade/tachiyomi/animesource/model/SAnime;
    .param p2, "$completion"    # Lkotlin/coroutines/Continuation;

    invoke-interface {p1}, Leu/kanade/tachiyomi/animesource/model/SAnime;->getUrl()Ljava/lang/String;
    move-result-object v0
    invoke-direct {p0, v0}, Leu/kanade/tachiyomi/animeextension/en/rule34video/Rule34Video;->normalizeUrl(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0

    # Warm up the site and collect the PHP/DDoS-Guard cookies before detail request.
    invoke-virtual {p0}, Leu/kanade/tachiyomi/animeextension/en/rule34video/Rule34Video;->getClient()Lokhttp3/OkHttpClient;
    move-result-object v1
    invoke-direct {p0, v0}, Leu/kanade/tachiyomi/animeextension/en/rule34video/Rule34Video;->popupUrl(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v2
    invoke-direct {p0, v0}, Leu/kanade/tachiyomi/animeextension/en/rule34video/Rule34Video;->pageHeaders(Ljava/lang/String;)Lokhttp3/Headers;
    move-result-object v3
    const/4 v4, 0x0
    const/4 v5, 0x4
    invoke-static {v2, v3, v4, v5, v4}, Leu/kanade/tachiyomi/network/RequestsKt;->GET$default(Ljava/lang/String;Lokhttp3/Headers;Lokhttp3/CacheControl;ILjava/lang/Object;)Lokhttp3/Request;
    move-result-object v2
    invoke-virtual {v1, v2}, Lokhttp3/OkHttpClient;->newCall(Lokhttp3/Request;)Lokhttp3/Call;
    move-result-object v1
    invoke-interface {v1}, Lokhttp3/Call;->execute()Lokhttp3/Response;
    move-result-object v1
    invoke-virtual {v1}, Lokhttp3/Response;->isSuccessful()Z
    move-result v3
    if-eqz v3, :popup_failed
    invoke-direct {p0, v1, p1}, Leu/kanade/tachiyomi/animeextension/en/rule34video/Rule34Video;->parseDetails(Lokhttp3/Response;Leu/kanade/tachiyomi/animesource/model/SAnime;)Leu/kanade/tachiyomi/animesource/model/SAnime;
    move-result-object v3
    invoke-virtual {v1}, Lokhttp3/Response;->close()V
    return-object v3

    :popup_failed
    invoke-virtual {v1}, Lokhttp3/Response;->close()V
    const/4 v2, 0x0

    invoke-virtual {p0}, Leu/kanade/tachiyomi/animeextension/en/rule34video/Rule34Video;->getClient()Lokhttp3/OkHttpClient;
    move-result-object v1
    invoke-direct {p0, v0}, Leu/kanade/tachiyomi/animeextension/en/rule34video/Rule34Video;->pageHeaders(Ljava/lang/String;)Lokhttp3/Headers;
    move-result-object v3
    if-eqz v2, :detail_headers_ready
    invoke-virtual {v3}, Lokhttp3/Headers;->newBuilder()Lokhttp3/Headers$Builder;
    move-result-object v3
    const-string v4, "Cookie"
    invoke-virtual {v3, v4, v2}, Lokhttp3/Headers$Builder;->set(Ljava/lang/String;Ljava/lang/String;)Lokhttp3/Headers$Builder;
    move-result-object v3
    invoke-virtual {v3}, Lokhttp3/Headers$Builder;->build()Lokhttp3/Headers;
    move-result-object v3

    :detail_headers_ready
    const/4 v4, 0x0
    const/4 v5, 0x4
    invoke-static {v0, v3, v4, v5, v4}, Leu/kanade/tachiyomi/network/RequestsKt;->GET$default(Ljava/lang/String;Lokhttp3/Headers;Lokhttp3/CacheControl;ILjava/lang/Object;)Lokhttp3/Request;
    move-result-object v0
    invoke-virtual {v1, v0}, Lokhttp3/OkHttpClient;->newCall(Lokhttp3/Request;)Lokhttp3/Call;
    move-result-object v0
    invoke-interface {v0}, Lokhttp3/Call;->execute()Lokhttp3/Response;
    move-result-object v0
    invoke-virtual {v0}, Lokhttp3/Response;->isSuccessful()Z
    move-result v1
    if-eqz v1, :detail_failed
    invoke-direct {p0, v0, p1}, Leu/kanade/tachiyomi/animeextension/en/rule34video/Rule34Video;->parseDetails(Lokhttp3/Response;Leu/kanade/tachiyomi/animesource/model/SAnime;)Leu/kanade/tachiyomi/animesource/model/SAnime;
    move-result-object v1
    invoke-virtual {v0}, Lokhttp3/Response;->close()V
    return-object v1

    :detail_failed
    invoke-virtual {v0}, Lokhttp3/Response;->close()V
    return-object p1
.end method'''
# Correct the accidental class descriptor in the generated invoke and keep the original model descriptor.
new_request = new_request.replace('Leu/kanade/tachiyomi/animeextension/en/rule34video/Rule34Video;)Leu/kanade/tachiyomi/animesource/model/SAnime;', 'Leu/kanade/tachiyomi/animesource/model/SAnime;)Leu/kanade/tachiyomi/animesource/model/SAnime;')
patched = patched[:request_start] + new_request + patched[request_end:]
path.write_text(patched)
print(f'patched {path}')
