from pathlib import Path

source = (Path(__file__).parent / 'src' / 'Rule34Video.kt').read_text(encoding='utf-8')

required = {
    'search': '"/search/"',
    'sort': 'sort_by',
    'date': 'date',
    'model': 'model_ids',
    'category': 'category_ids',
    'tag': 'tag_ids',
    'uploader': 'uploader',
    'minimum_duration': 'min_duration',
    'maximum_duration': 'max_duration',
    'blacklist': 'blacklist',
    'popup_details': 'override fun animeDetailsRequest',
    'popup_playback': 'override fun videoListRequest',
    'video_links': "a.tag_item.tag_item_download[href*='/get_file/']",
    'session_headers': 'mediaHeaders(response)',
}
for name, marker in required.items():
    assert marker in source, f'missing contract: {name} -> {marker}'
assert source.count('class ') >= 10
print('PASS: source contracts for search, filters, details, and playback')
