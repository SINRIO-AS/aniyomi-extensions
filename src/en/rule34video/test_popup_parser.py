from pathlib import Path
from bs4 import BeautifulSoup

fixture = Path('/home/ubuntu/browser_html/rule34video_com_4556460_1787112043754.html')
html = fixture.read_text(encoding='utf-8', errors='ignore')
soup = BeautifulSoup(html, 'html.parser')

title = soup.select_one('h1.title_video')
description = soup.select_one('#tab_video_info > .row .label em')
categories = [a.get_text(' ', strip=True) for a in soup.select("a.item.btn_link.video_meta_pill[href*='/categories/']")]
artists = [a.get_text(' ', strip=True) for a in soup.select("a.item.btn_link.video_meta_pill[href*='/models/']")]
uploaders = [a.get_text(' ', strip=True) for a in soup.select("a[href*='/members/']")]
tags = [a.get_text(' ', strip=True) for a in soup.select("a.tag_item[href*='/tags/']")]
qualities = [(a.get_text(' ', strip=True), a.get('href', '')) for a in soup.select("a.tag_item.tag_item_download[href*='/get_file/']")]

assert title and 'Sex on the Beach' in title.get_text()
assert description and 'fansaiko.com' in description.get_text()
assert categories == ['Nikke: Goddess of Victory']
assert artists == ['rinHsu']
assert uploaders == ['rinHsu']
assert len(tags) >= 10
assert [q[0] for q in qualities] == ['MP4 1080p', 'MP4 720p', 'MP4 480p', 'MP4 360p']
assert all('/get_file/' in href and '.mp4' in href for _, href in qualities)
print('PASS: popup parser fixture')
print({'title': title.get_text(' ', strip=True), 'categories': categories, 'artists': artists, 'uploader': uploaders, 'tag_count': len(tags), 'quality_count': len(qualities)})
