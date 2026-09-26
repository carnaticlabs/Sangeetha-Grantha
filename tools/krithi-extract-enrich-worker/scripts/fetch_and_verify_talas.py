#!/usr/bin/env python3
import json
import os
import re
import time
import unicodedata
import urllib.request
from bs4 import BeautifulSoup
from difflib import SequenceMatcher

CACHE_DIR = "output/karnatik-cache"
os.makedirs(CACHE_DIR, exist_ok=True)

def super_norm(s):
    if not s:
        return ""
    s = unicodedata.normalize('NFKD', s.casefold())
    for c in ['\u0300', '\u0301', '\u0302', '\u0303', '\u0304', '\u0306', '\u0307', '\u0308', '\u030a', '\u030b', '\u030c', '\u030d', '\u0323', '\u0324', '\u0325', '\u0327', '\u0328']:
        s = s.replace(c, '')
    s = s.replace('ae', 'e').replace('ai', 'e').replace('ch', 'c').replace('th', 't').replace('dh', 'd')
    s = s.replace('bh', 'b').replace('kh', 'k').replace('gh', 'g').replace('ph', 'p').replace('sh', 's')
    s = s.replace('ee', 'i').replace('oo', 'u').replace('aa', 'a').replace('ou', 'u')
    res = []
    for char in s:
        if not char.isalnum():
            continue
        if res and res[-1] == char:
            continue
        res.append(char)
    return ''.join(res)

# Load karnatik songs index
with open('/Users/seshadri/.gemini/antigravity/brain/7d7bedef-840e-4d89-a175-668ca3e18a9f/.system_generated/steps/95/content.md') as f:
    soup = BeautifulSoup(f.read(), 'html.parser')

karnatik_songs = []
for a in soup.find_all('a', href=True):
    href = a['href']
    if re.match(r'^c\d+\.shtml$', href):
        raw = a.get_text().strip()
        parts = raw.split(' - ')
        title = parts[0].strip()
        raga = parts[1].strip() if len(parts) > 1 else ''
        karnatik_songs.append({
            'href': href,
            'raw': raw,
            'title': title,
            'raga': raga,
            'norm_title': super_norm(title),
            'norm_raga': super_norm(raga)
        })

prompt_table = '''Aada Modi Galadae	Chārukesi
Aanandamaananda	Bhairavi
Abhimaanamennadu	Kunjari
Abhimaanamu Ledemi	Andhali
ADavAramella	Yadukula Kāmbhoji
adi kAdu bhajana	Yadukula Kāmbhoji
Anaathudanu	Jingla
Anyaayamu	Kāpi
Aparaadhamula Maanpi	Darbar
Atade Dhanyudu	Kāpi
Baale Baalendu	Reethigowla
Badalika Teera	Reethigowla
bhajana parulakEla	suraTi
bhajana sEyu mArgamu	Nārāyani
Bhaja Ramam	Huseni
Bhuvini Dasudanae	Ranjani
Brocevaarevarae	Ranjani
buddhi rAdu	SankarAbharaNaM
celimini jalajAkshu	Yadukula Kāmbhoji
Chalamelaraa	mArgahindOLaM
Chani Todi Tevae	Harikāmbhōji
Chentanae Sadaa	Kunthalavarāli
Chera Raavademi	Reethigowla
Chetulaara	Kharaharapriyā
Chinna Naade	Kalānidhi
Chinthisthunnaadae	Mukhāri
Chootaamu Raareyee Vedkanu	Kāpi
daNDamu	Bālahamsa
darSanamu sEya	nArAyaNa gauLa
daya rAni	Mohanam
daya sEyavayya	Yadukula Kāmbhoji
dEva rAma rAma	saurAshTraM
Deva Sri Tapasteertha	Madhyamāvathi
Dharmaatma	kEdAra gauLa
Edi Nee Baahu Bala	Darbar
Elaavataara	Mukhāri
Elaraa Sri Krishnaa	Kāmbhoji
Ela Teliya Lero	Darbar
Emani Pogaduduraa	Veeravasantham
Emani Vegintunae	Huseni
Emi nEramu	SankarAbharaNaM
Endu Kaugalinturaa	Shuddha Desi
endukI calamu	SankarAbharaNaM
endukO bAga	Mohanam
Ennaallu Nee Trova	Kāpi
Ennaallu Tirigedi	Mālavashree
Enta Nerchina	Suddha Dhanyāsi
entanucu sairintunu	Yadukula Kāmbhoji
enta vEDukondu	Saraswathi Manohari
E tAvuna nErcitivO	Yadukula Kāmbhoji
eTulaina bhakti	sAma
Evaraina Leraa	Siddhasena
Evarani Nirnayinchiri	Devamrta Varshini
Evaricchiriraa	Madhyamāvathi
Evarikai	Deva Manohari
Evarunnaaru Brova	Mālavashree
E vidhamulanaina	SankarAbharaNaM
gandhamu puyyarugA	Punnagavarali
Garuda Gamana	Gourimanohari
gata mOhASrita	SankarAbharaNaM
Giripai Nelakonna	Sahāna
heccarikagA	Yadukula Kāmbhoji
ika kAvalasinadEmi	Bālahamsa
Indukaa Puttinchitivi	Bhairavi
Indukaayee Tanuvunu Penchinadi	Mukhāri
Inkaa Daya	nArAyaNa gauLa
innALLu daya	nArAyaNa gauLa
Inta Saukhyamani	Kāpi
itara daivamulu	Chāyatārangini
Ivaraku jUcinadi	SankarAbharaNaM
Ivasudha Neevanti	Sahāna
Jo Jo Rama	Reethigowla
kadaluvADu	nArAyaNa gauLa
Kali Narulaku	Kunthalavarāli
Kalugunaa	Poornalalita
Kamalaapta Kula	bRndAvana sAranga
Kanna Tandri	Deva Manohari
Kanulu Taakani	Kalyāna Vasantam
Karunaa Jaladhi	kEdAra gauLa
Koluvaiyunnaadae	Bhairavi
koniyADE	Kokiladhwani
Kori Sevimpa	Kharaharapriyā
Krpaalavaala	Nādavarangini
kRpa jUcuTaku	Chāyatārangini
Ksheera Saagara Vihaara	Anandabhairavi
Kula Birudunu	Deva Manohari
kuvalaya daLa	nATa kuranji
Laali Laalayya	kEdAra gauLa
Laali Laaliyani	Harikāmbhōji
Laavanya Rama	Purna Shadjam
Lalitae Sri Pravriddhae	Bhairavi
Maa Ramachandruniki	kEdAra gauLa
Maaru Palkaga	Ranjani
Mahita Pravrddha	Kāmbhoji
mAkElarA vicAramu	Ravi Chandrikā
mA kulamuna	suraTi
Manasaa Sri Ramachandruni	Eeshamanohari
Manasaa Sri Ramuni	Māraranjani
Manasu Loni Marmamu	Hindolam
Manasu Nilpa	Abhogi
manasu vishaya	nATa kuranji
Manavini Vinumaa	Jayanārāyani
Mari Mari Ninnae	Kāmbhoji
mariyAda kAdurA	SankarAbharaNaM
mATi mATiki	Mohanam
Meevalla Guna Dosha	Kāpi
Menu Joochi	Sarasāngi
Mokshamu Galadaa	Sāramati
Mridu Bhaashana	Maruvadhanyāsi
Mucchata	Madhyamāvathi
Muripemu	Mukhāri
Naada Loludai	Kalyāna Vasantam
Naama Kusuma	Sri
Naati Maata	Devakriya
Nadachi Nadachi	Kharaharapriyā
Nagu Momu Gala Vaani	Madhyamāvathi
Nagu Momu Kana Leni	Abheri
Nalina Lochana	Madhyamāvathi
Nammina Vaarini	Bhairavi
nannu brOvakanu	SankarAbharaNaM
Nannu Brova Neekinta	Abhogi
nannu kanna talli	kEsari - sindhu kannaDa
Nannu Vidichi	Reethigowla
nApAli SrI rAma	SankarAbharaNaM
Narada Guru Svami	Darbar
Natha Brovavae	Bhairavi
Nee Bhajana	Nāyaki
Nee Bhakti	Jayamanohari
Nee Daya Kalgute	Reethigowla
Neeke Teliyaka	Anandabhairavi
Nee Muddu Momu	Kamalā Manohari
Neevanti Daivamu	Bhairavi
Nenaruncharaa Naapaini	Simhavāhini
nenaruncinAnu	Mālavi
Nenendu Vetukuduraa	Karnātaka Behāg
nI dayacE rAma	Yadukula Kāmbhoji
Nijamaitae Mundara	Bhairavi
Nija Marmamula	Umābharanam
Nijamuga Nee	Sahāna
ninu bAsi	Bālahamsa
ninu vinA nA madi	Navarasa kannada
niravadhi sukhada	ravi candirka
Nitya Roopa	Kāpi
O Jagannaatha	kEdAra gauLa
Oka Maata	Harikāmbhōji
Ora Joopu	kannaDa gauLa
O Rama Rama Sarvonnata	Nāgagāndhāri
Paahi Kalyaana Rama	Kāpi
Paahi Mam Sri Ramachandra	Kāpi
Paahi Parama Dayaalo	Kāpi
Paahi Rama Ramayanucu	Kharaharapriyā
pAhi rAma candra	Yadukula Kāmbhoji
Pakkala Nilabadi	Kharaharapriyā
paluka kaNDa	Navarasa kannada
Paraaku Neekelaraa	Keeranāvali
parAmukhamEla	suraTi
Paripaalaya Paripaalaya	Reethigowla
paripAlaya dASarathE	SankarAbharaNaM
Paripalaya Mam	Darbar
Paritaapamu	Manohari
parulanu vEDanu	Bālahamsa
patiki hArati	suraTi
Patti Viduva	Manjari
Peridi Ninu	Kharaharapriyā
Phanipati Sayi	Jhankāradhvani
prArabdham	Swarāvali
Raamuni Maravakavae	kEdAra gauLa
Raanidi Raadu	Manirangu
Raaraa Phani Sayana	Harikāmbhōji
Raaraa Seetha Ramani	Hindolavasanta
Raga Ratna	Reethigowla
Raga Sudhaa	Andolikā
Raghu Nandana Raaja	Shuddha Desi
Raghu Nandana Raghu Nandana	kEdAra gauLa
Raghupatae Rama	Sahāna
Raghu Veera Rana	Huseni
Raksha Pettarae	Bhairavi
Rama Bhakti	Suddha Bangāla
rAmacandra nI daya	suraTi
rAma daivamA	suraTi
rAma Eva daivataM	Bālahamsa
Rama Kothanda Rama	Bhairavi
Rama Lobhamela	Darbar
Rama Namam Bhajarae	Madhyamāvathi
Rama Neeyeda	Kharaharapriyā
Rama Ninnae Nammi	Huseni
rAma ninu vinA	SankarAbharaNaM
rAma nIvE kAni	Nārāyani
Rama Paahi	Kāpi
Rama Raghu Kula	Kāpi
rAma rAma gOvinda	saurAshTraM
ramA ramaNa rArA	SankarAbharaNaM
Rama Rama Nee Vaaramu	Anandabhairavi
Rama Rama Rama Laali	Sahāna
rAma rAma rAma rAma	cencuruTi
rAmA rAma rAma rAmAyani	Mohanam
Rama Samayamu	Madhyamāvathi
rAma sItA rAma	Bālahamsa
rAma SrI rAma lAli	SankarAbharaNaM
ramincuvArevarurA	Suposhini
ranga nAyaka	SankarAbharaNaM
rArA mAyiNTidAka	Asāveri
SambhO Siva	SankarAbharaNaM
Samukhaana Nilva	Kokilavarāli
Sanaatana	Phalamanjari
Sangita Sastra	Mukhāri
SAntamu lEka	sAma
Saranu Sarananucu	Madhyamāvathi
Sara Sara Samaraika	Kunthalavarāli
Saraseeruhaanana	Mukhāri
Sariyevvarae	Ranjani
sArvabhauma	rAga panjaraM
Sarva Loka Dayaanidhae	Huseni
Sarvantaryaami	Bhairavi
Siggu Maali	kEdAra gauLa
sItA pati kAvavayya	SankarAbharaNaM
Sita Pati Naa Manasuna	Kamās
Sogasugaa Mridanga	Ranjani
Sogasu Jooda	kannaDa gauLa
Sri Janaki Manohari	Eeshamanohari
Sri Maanini	Purna Shadjam
Sri Narada Muni	Bhairavi
SrI nArasiMha	Phalaranjani
SrIpaptE nI pada	Nāgaswarāvali
Sri Raghuvara Aprameya	Kāmbhoji
SrI raghu vara dASarathE	SankarAbharaNaM
Sri Raghuvara Sugunaalaya	Bhairavi
Sri Rama Jaya Rama	Madhyamāvathi
Sri Rama Paadamaa	Amrta Vahini
SrI rAma raghu rAma	Yadukula Kāmbhoji
Sri Rama Sri Rama Sri Manoharamaa	Sahāna
Sri Ramya Chitta	Jayamanohari
SRngArincukoni	suraTi
Sukhiyevaro	Kanadā
Sundara Dasaratha	Kāpi
Talachinantanae	Mukhāri
talli taNDrulu	Bālahamsa
Tanayandae Prema	Bhairavi
Tanayuni Brova	Bhairavi
Tappaganae	Suddha Bangāla
Toli Ne Jesina	Suddha Bangāla
toli nEnu jEsina	Kokiladhwani
Tulasi Bilva	kEdAra gauLa
Undedi Ramudu	Harikāmbhōji
Upachaaramulanu Chekona	Bhairavi
Urakae Kalgunaa	Sahāna
Vaarija Nayana-1	kEdAra gauLa
Vaarija Nayana-2	kEdAra gauLa
valla kAdanaka	SankarAbharaNaM
Vanaja Nayanudani	kEdAra gauLa
Vandanamu	Sahāna
varadA navanItASa	rAga panjaraM
Varada Raja	Swarabhooshani
vara lIla gAna	SankarAbharaNaM
Vara Raaga Laya	Cencukambhoji
Vara Sikhi Vaahana	Supradipama
vEda vAkyamani	Mohanam
Venkatesa	Madhyamāvathi
vErevvarE gati	suraTi
Videmu Seyavae	Kharaharapriyā
vinanAsakoni	Pratāpavarāli
Vinataa Suta Raaraa	Huseni
Vinataa Suta Vaahanudai	Harikāmbhōji
virAja turaga	Bālahamsa
vishNu vAhanuDu	SankarAbharaNaM
Yajnaadulu	Jayamanohari
Yochanaa Kamala	Darbar
Yuktamu Kaadu	Sri'''

items = [line.split('\t') for line in prompt_table.strip().split('\n')]

SPECIAL_OVERRIDES = {
    'Krpaalavaala': ('c2321.shtml', 'nrpaalavaala kalaadhara - naadavarangini'),
    'Nagu Momu Kana Leni': ('c1001.shtml', 'nagumOmu ganalEni - aabhEri'),
    'sArvabhauma': ('c2409.shtml', 'saarvabhowma saakETa - raagapanjaramu'),
    'Sri Narada Muni': ('c2443.shtml', 'shree naarada gururaaya - bhairavi'),
    'SrIpaptE nI pada': ('c2448.shtml', 'shreepatE - naagaswaraavaLi'),
}

def get_page_content(href):
    local_path = os.path.join(CACHE_DIR, href)
    if os.path.exists(local_path):
        with open(local_path, 'r', encoding='utf-8', errors='ignore') as f:
            return f.read()
    url = f"https://www.karnatik.com/{href}"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            content = resp.read().decode('utf-8', errors='ignore')
        with open(local_path, 'w', encoding='utf-8') as f:
            f.write(content)
        time.sleep(0.04)
        return content
    except Exception as e:
        print(f"Error fetching {url}: {e}")
        return None

def parse_song_page(html):
    if not html:
        return None, None
    soup = BeautifulSoup(html, 'html.parser')
    text = soup.get_text()
    raga = None
    tala = None
    for line in text.split('\n'):
        line = line.strip()
        if not line:
            continue
        lower = line.lower()
        if lower.startswith('raagam:') or lower.startswith('ragam:'):
            raga = line.split(':', 1)[1].strip()
        elif lower.startswith('taalam:') or lower.startswith('taaLam:'):
            tala = line.split(':', 1)[1].strip()
    return raga, tala

def canonical_tala(raw):
    if not raw:
        return "Unknown"
    norm = super_norm(raw)
    if 'desadi' in norm or 'deshadi' in norm:
        return "Adi (Deshadi)"
    if 'madhyadi' in norm:
        return "Adi (Madhyadi)"
    if 'adi' in norm:
        return "Adi"
    if 'rupaka' in norm or 'rupak' in norm or 'rupakam' in norm:
        return "Rupaka"
    if 'misracapu' in norm or 'misrachapu' in norm or 'mishracapu' in norm:
        return "Misra Capu"
    if 'khandacapu' in norm or 'khandachapu' in norm:
        return "Khanda Capu"
    if 'jhampa' in norm:
        return "Jhampa"
    if 'triputa' in norm:
        return "Triputa"
    if 'ata' in norm:
        return "Ata"
    if 'ekam' in norm:
        return "Ekam"
    return raw

results = []

for idx, (title, cat_raga) in enumerate(items, 1):
    snt = super_norm(title)
    snr = super_norm(cat_raga)
    
    if title in SPECIAL_OVERRIDES:
        href, desc = SPECIAL_OVERRIDES[title]
    else:
        candidates = []
        for ks in karnatik_songs:
            knt = ks['norm_title']
            knr = ks['norm_raga']
            if snt == knt or snt.startswith(knt) or knt.startswith(snt):
                t_score = 1.0
            else:
                t_score = SequenceMatcher(None, snt, knt).ratio()
            if snr == knr or snr in knr or knr in snr:
                r_score = 1.0
            else:
                r_score = SequenceMatcher(None, snr, knr).ratio()
            candidates.append((t_score * 0.7 + r_score * 0.3, t_score, r_score, ks))
        candidates.sort(key=lambda x: x[0], reverse=True)
        best = candidates[0]
        href = best[3]['href']
        desc = best[3]['raw']
        
    html = get_page_content(href)
    raga, tala = parse_song_page(html)
    canon = canonical_tala(tala)
    results.append({
        'index': idx,
        'title': title,
        'raga': cat_raga,
        'matched_href': href,
        'matched_desc': desc,
        'source_raga': raga,
        'source_tala': tala,
        'canonical_tala': canon
    })
    if idx % 25 == 0 or idx == len(items):
        print(f"Processed {idx}/{len(items)}: {title} -> {canon} (raw: {tala})")

with open('output/resolved_tyagaraja_talas.json', 'w') as f:
    json.dump(results, f, indent=2)

print("\nDone! Results saved to output/resolved_tyagaraja_talas.json")
