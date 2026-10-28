#!/usr/bin/env python3
"""
Haalt merklogo's op van Wikimedia Commons voor de auto's in Autosim, kiest per auto de
logoversie die bij het bouwjaar past en zet ze om naar een vast formaat, in twee versies:
  - kleur: PNG van 256 x 256 pixels, transparante achtergrond, logo gecentreerd;
  - mono:  dezelfde PNG in één kleur, het oranje van de app op een zwarte achtergrond.

Gebruik (vanuit de hoofdmap van de repo):

    pip install requests pillow cairosvg
    python3 tools/logos.py                 # alle merken
    python3 tools/logos.py --merk FERRARI  # alleen één merk
    python3 tools/logos.py --droog         # alleen zoeken en tonen, niets downloaden
    python3 tools/logos.py --stijl mono    # de app de monochrome versie laten tonen (of: kleur)

Resultaat:
    assets/logos/*.png            de omgezette logo's in kleur
    assets/logos/*-mono.png       dezelfde logo's monochroom: oranje op zwart
    assets/logos/logos.json       welke auto welk logo gebruikt (de app leest dit)
    assets/logos/BRONNEN.md       per logo het bestand op Commons, de maker en de licentie
    tools/logos-overzicht.html    overzicht om de keuzes te controleren: beide versies naast elkaar,
                                  en alle kandidaten per merk

Welke versie de app laat zien, staat in logos.json ("stijl": "kleur" of "mono"). Je wisselt
zonder opnieuw te downloaden met: python3 tools/logos.py --stijl mono --alleen-stijl

Klopt een keuze niet, zet dan in tools/logo-keuzes.json welk Commons-bestand een merk vanaf
welk jaar moet gebruiken, en draai het script opnieuw. Voorbeeld:

    {
      "FERRARI": [{"vanaf": 1947, "bestand": "File:Ferrari-Logo.svg"}],
      "ALFA ROMEO": [{"vanaf": 1950, "bestand": "File:..."}, {"vanaf": 1972, "bestand": "File:..."}]
    }

Let op: logo's zijn handelsmerken van hun eigenaars. Voor eigen gebruik in je app is dit
meestal geen probleem; als je de app of de logo's openbaar verspreidt, controleer dan de
licentie en de merkrechten (BRONNEN.md helpt daarbij). De logo-bestanden staan daarom
standaard in .gitignore.
"""

import argparse
import html
import io
import json
import re
import sys
import time
from pathlib import Path

try:
    import requests
    from PIL import Image
except ImportError:
    sys.exit("Installeer eerst de benodigde pakketten: pip install requests pillow cairosvg")

API = "https://commons.wikimedia.org/w/api.php"
# Wikimedia vraagt een herkenbare User-Agent met een contactmogelijkheid; vul gerust je eigen gegevens in.
HEADERS = {"User-Agent": "AutosimLogoFetcher/1.0 (persoonlijk project; https://github.com/rarevogel)"}
SIZE = 256          # uitvoer: vierkant van SIZE x SIZE pixels
MARGIN = 0.08       # lege rand rondom het logo, als deel van SIZE
ORANGE = (0xFF, 0x8A, 0x1F)   # het accentoranje van de app (App.ACCENT)
BLACK = (0, 0, 0)

ROOT = Path(__file__).resolve().parent.parent
SOURCES = [ROOT / "src/nl/rarevogel/autosim/core/Car.java", ROOT / "src/nl/rarevogel/autosim/core/CarsMore.java"]

# Autonaam (begin) -> merk zoals gezocht op Commons. Langste voorvoegsel wint.
BRANDS = {
    "ALFA ROMEO": "Alfa Romeo", "ALPINE": "Alpine", "ASTON MARTIN": "Aston Martin", "AUDI": "Audi",
    "AUSTIN-HEALEY": "Austin-Healey", "BENTLEY": "Bentley", "BMW ALPINA": "Alpina", "BMW": "BMW",
    "BUGATTI": "Bugatti", "CHEVROLET": "Chevrolet", "CHEVROLETTE": "Chevrolet", "CITROEN": "Citroën",
    "DATSUN": "Datsun", "DE TOMASO": "De Tomaso", "PANTERA": "De Tomaso", "DELOREAN": "DeLorean",
    "DODGE": "Dodge", "DONKERVOORT": "Donkervoort", "FERRARI": "Ferrari", "FIAT": "Fiat", "FORD": "Ford",
    "HONDA": "Honda", "JAGUAR": "Jaguar", "LAMBORGHINI": "Lamborghini", "LANCIA": "Lancia", "LEXUS": "Lexus",
    "LOTUS": "Lotus", "MASERATI": "Maserati", "MAZDA": "Mazda", "MCLAREN": "McLaren", "MERCEDES": "Mercedes-Benz",
    "MERCEDES-BENZ": "Mercedes-Benz", "MERCEDES-AMG": "Mercedes-AMG", "MG": "MG", "MINI": "Mini", "MITSUBISHI": "Mitsubishi", "NISSAN": "Nissan",
    "OPEL": "Opel", "PEUGEOT": "Peugeot", "PLYMOUTH": "Plymouth", "PONTIAC": "Pontiac", "PORSCHE": "Porsche",
    "RENAULT": "Renault", "SAAB": "Saab", "SHELBY": "Shelby", "SUBARU": "Subaru", "TOYOTA": "Toyota",
    "VOLKSWAGEN": "Volkswagen", "VW": "Volkswagen",
}


# ------------------------------------------------------------------ auto's uit de broncode

def read_cars():
    """Leest naam en bouwjaar van alle auto's uit Car.java en CarsMore.java."""
    cars = {}
    original = SOURCES[0].read_text(encoding="utf-8")
    for name in re.findall(r'add\("(.{30})', original):
        cars[name.strip()] = None
    for name, year in re.findall(r'setYear\("([^"]+)",\s*(\d{4})\)', original):
        cars[name] = int(year)
    more = SOURCES[1].read_text(encoding="utf-8")
    for name, year in re.findall(r'a\("([^"]+)",\s*"[^"]*"\)\.year\((\d{4})\)', more):
        cars[name] = int(year)
    missing = [n for n, y in cars.items() if y is None]
    if missing:
        print("Waarschuwing: geen bouwjaar gevonden voor", ", ".join(missing))
    return {n: y for n, y in cars.items() if y is not None}


def brand_of(name):
    best = None
    for prefix, brand in BRANDS.items():
        if (name == prefix or name.startswith(prefix + " ")) and (best is None or len(prefix) > len(best[0])):
            best = (prefix, brand)
    return best[1] if best else None


def slug(s):
    s = s.lower().replace("ë", "e").replace("é", "e")
    return re.sub(r"[^a-z0-9]+", "-", s).strip("-")


# ------------------------------------------------------------------ Commons

def api(params):
    params = dict(params, format="json", formatversion=2)
    for attempt in range(4):
        r = requests.get(API, params=params, headers=HEADERS, timeout=30)
        if r.status_code == 429 or r.status_code >= 500:
            time.sleep(2 * (attempt + 1))
            continue
        r.raise_for_status()
        return r.json()
    r.raise_for_status()


def strip_html(s):
    return html.unescape(re.sub(r"<[^>]+>", "", s or "")).strip()


def years_in(text):
    """Geeft (vanaf, tot) uit een titel of beschrijving: '1947-1958' -> (1947, 1958), '1972' -> (1972, None)."""
    m = re.search(r"\b(1[89]\d\d|20[0-4]\d)\s*[-–]\s*(1[89]\d\d|20[0-4]\d|present|heden|today)\b", text, re.I)
    if m:
        end = m.group(2)
        return int(m.group(1)), int(end) if end.isdigit() else None
    m = re.search(r"\b(1[89]\d\d|20[0-4]\d)\b", text)
    return (int(m.group(1)), None) if m else (None, None)


def search_logos(brand, limit=25):
    """Zoekt logobestanden van een merk op Commons, met licentie en maker."""
    data = api({
        "action": "query", "generator": "search", "gsrnamespace": 6, "gsrlimit": limit,
        "gsrsearch": f'intitle:"{brand}" intitle:logo',
        "prop": "imageinfo", "iiprop": "url|mime|size|extmetadata", "iiurlwidth": 400,
    })
    out = []
    for page in (data.get("query") or {}).get("pages", []):
        info = (page.get("imageinfo") or [{}])[0]
        mime = info.get("mime", "")
        if mime not in ("image/svg+xml", "image/png", "image/jpeg", "image/gif", "image/webp"):
            continue
        title = page["title"]
        lowered = title.lower()
        # geen foto's van emblemen op auto's, geen sponsorlogo's van teams
        if any(w in lowered for w in ("photo", "foto", "badge on", "emblem on", "racing team", "f1 team", "dealer")):
            continue
        meta = info.get("extmetadata", {})
        desc = strip_html(meta.get("ImageDescription", {}).get("value", ""))
        start, end = years_in(title)
        if start is None:
            start, end = years_in(desc[:200])
        out.append({
            "title": title,
            "url": info.get("url"),
            "thumb": info.get("thumburl") or info.get("url"),
            "page": info.get("descriptionurl"),
            "mime": mime,
            "license": strip_html(meta.get("LicenseShortName", {}).get("value", "")) or "onbekend",
            "artist": strip_html(meta.get("Artist", {}).get("value", "")) or "onbekend",
            "trademarked": "trademark" in json.dumps(meta).lower(),
            "from": start, "to": end,
            "index": page.get("index", 999),
        })
    out.sort(key=lambda c: c["index"])
    return out


def file_info(title):
    """Gegevens van één opgegeven Commons-bestand (voor handmatige keuzes)."""
    data = api({"action": "query", "titles": title, "prop": "imageinfo", "iiprop": "url|mime|extmetadata", "iiurlwidth": 400})
    page = data["query"]["pages"][0]
    if "missing" in page:
        raise SystemExit(f"Bestand bestaat niet op Commons: {title}")
    info = page["imageinfo"][0]
    meta = info.get("extmetadata", {})
    return {
        "title": page["title"], "url": info["url"], "thumb": info.get("thumburl") or info["url"],
        "page": info.get("descriptionurl"), "mime": info["mime"],
        "license": strip_html(meta.get("LicenseShortName", {}).get("value", "")) or "onbekend",
        "artist": strip_html(meta.get("Artist", {}).get("value", "")) or "onbekend",
        "trademarked": "trademark" in json.dumps(meta).lower(),
    }


# ------------------------------------------------------------------ keuze per bouwjaar

def choose(candidates, year):
    """
    Kiest de logoversie voor een bouwjaar: de jongste versie die op of voor dat jaar begon (en
    niet al was afgelopen); anders een versie zonder jaartal (meestal het huidige logo); anders
    de oudste die er is. SVG gaat voor bij een gelijke keuze.
    """
    def svg_first(c):
        return 0 if c["mime"] == "image/svg+xml" else 1

    dated = [c for c in candidates if c["from"] is not None and c["from"] <= year and (c["to"] is None or c["to"] >= year)]
    if dated:
        return sorted(dated, key=lambda c: (-c["from"], svg_first(c), c["index"]))[0]
    undated = [c for c in candidates if c["from"] is None]
    if undated:
        return sorted(undated, key=lambda c: (svg_first(c), c["index"]))[0]
    later = [c for c in candidates if c["from"] is not None]
    return sorted(later, key=lambda c: (c["from"], svg_first(c)))[0] if later else None


# ------------------------------------------------------------------ omzetten

def to_standard_png(raw, mime):
    """Zet een logo om naar een vierkante PNG van SIZE pixels met transparante achtergrond."""
    return png_bytes(to_standard_image(raw, mime))


def png_bytes(img):
    buf = io.BytesIO()
    img.save(buf, "PNG", optimize=True)
    return buf.getvalue()


def to_mono(img):
    """
    Monochrome versie: het logo in het oranje van de app op een zwarte achtergrond.
    Donkere delen van het logo worden vol oranje, lichte delen (zoals witte letters of vlakken)
    zwart; tussentinten worden half oranje. Heeft het logo maar één kleur, dan wordt het
    helemaal oranje. Randen blijven glad doordat ook de transparantie meetelt.
    """
    rgba = img.convert("RGBA")
    lum = rgba.convert("L")
    alpha = rgba.getchannel("A")
    # lichtheidsbereik van de zichtbare pixels bepalen, voor een volle contrastschaal
    visible = [l for l, a in zip(lum.getdata(), alpha.getdata()) if a > 128]
    if visible:
        visible.sort()
        lo = visible[len(visible) // 50]            # 2% donkerste, om losse pixels te negeren
        hi = visible[len(visible) - 1 - len(visible) // 50]
    else:
        lo, hi = 0, 255
    span = hi - lo
    if span < 40:
        # (vrijwel) één kleur: alles wat zichtbaar is wordt oranje
        strength = alpha
    else:
        inv = lum.point(lambda l: max(0, min(255, round((hi - l) * 255 / span))))
        from PIL import ImageChops
        strength = ImageChops.multiply(inv, alpha)
    out = Image.new("RGB", rgba.size, BLACK)
    out.paste(Image.new("RGB", rgba.size, ORANGE), (0, 0), strength)
    return out


def to_standard_image(raw, mime):
    """Logo als vierkante RGBA-afbeelding van SIZE pixels, transparante achtergrond, zonder lege randen."""
    if mime == "image/svg+xml":
        try:
            import cairosvg
        except ImportError:
            sys.exit("Voor SVG-logo's is cairosvg nodig: pip install cairosvg")
        raw = cairosvg.svg2png(bytestring=raw, output_width=SIZE * 4)
    img = Image.open(io.BytesIO(raw)).convert("RGBA")

    # bijsnijden: weg met transparante randen, en met een effen witte achtergrond rondom
    px = img.load()
    w, h = img.size
    bg = px[0, 0]
    plain_white = bg[3] > 0 and min(bg[:3]) > 245
    if plain_white:
        # alleen een witte achtergrond die aan de rand vastzit transparant maken (flood fill),
        # zodat wit binnen het logo blijft
        from PIL import ImageDraw
        for corner in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
            if min(px[corner][:3]) > 245 and px[corner][3] > 0:
                ImageDraw.floodfill(img, corner, (255, 255, 255, 0), thresh=12)
    box = img.getchannel("A").getbbox()
    if box:
        img = img.crop(box)

    inner = int(SIZE * (1 - 2 * MARGIN))
    scale = min(inner / img.width, inner / img.height)
    img = img.resize((max(1, round(img.width * scale)), max(1, round(img.height * scale))), Image.LANCZOS)
    out = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    out.paste(img, ((SIZE - img.width) // 2, (SIZE - img.height) // 2), img)
    return out


def download(url):
    r = requests.get(url, headers=HEADERS, timeout=60)
    r.raise_for_status()
    return r.content


# ------------------------------------------------------------------ hoofdprogramma

def main():
    ap = argparse.ArgumentParser(description="Haalt merklogo's op van Wikimedia Commons voor Autosim.")
    ap.add_argument("--merk", help="alleen dit merk, zoals het in de autonaam staat (bv. FERRARI of 'ALFA ROMEO')")
    ap.add_argument("--droog", action="store_true", help="alleen zoeken en de keuzes tonen, niets downloaden")
    ap.add_argument("--uit", default=str(ROOT / "assets/logos"), help="map voor de logo's (standaard assets/logos)")
    ap.add_argument("--stijl", choices=("kleur", "mono"), help="welke versie de app toont (standaard: wat er al stond, anders kleur)")
    ap.add_argument("--alleen-stijl", action="store_true", help="alleen de stijl in logos.json aanpassen, niets zoeken of downloaden")
    args = ap.parse_args()

    out_dir = Path(args.uit)
    if args.alleen_stijl:
        if not args.stijl:
            sys.exit("Geef met --stijl aan welke versie de app moet tonen: kleur of mono.")
        mapping_file = out_dir / "logos.json"
        if not mapping_file.exists():
            sys.exit(f"{mapping_file} bestaat nog niet; draai het script eerst zonder --alleen-stijl.")
        mapping = json.loads(mapping_file.read_text(encoding="utf-8"))
        mapping["stijl"] = args.stijl
        mapping_file.write_text(json.dumps(mapping, ensure_ascii=False, indent=1, sort_keys=True), encoding="utf-8")
        print(f"De app toont nu de versie '{args.stijl}'. Bouw de app opnieuw met ./build.sh.")
        return
    cars = read_cars()
    overrides_file = ROOT / "tools/logo-keuzes.json"
    overrides = json.loads(overrides_file.read_text(encoding="utf-8")) if overrides_file.exists() else {}

    by_brand = {}
    for name, year in sorted(cars.items(), key=lambda kv: kv[1]):
        brand = brand_of(name)
        if brand is None:
            print(f"Onbekend merk voor {name}; voeg het toe aan BRANDS in dit script.")
            continue
        by_brand.setdefault(brand, []).append((name, year))
    if args.merk:
        wanted = brand_of(args.merk.upper()) or args.merk
        by_brand = {b: v for b, v in by_brand.items() if b.lower() == wanted.lower()}
        if not by_brand:
            sys.exit(f"Geen auto's van merk '{args.merk}' gevonden.")

    mapping_file = out_dir / "logos.json"
    mapping = json.loads(mapping_file.read_text(encoding="utf-8")) if mapping_file.exists() else {"cars": {}, "files": {}}
    mapping["stijl"] = args.stijl or mapping.get("stijl", "kleur")
    report = []

    for brand, models in sorted(by_brand.items()):
        print(f"\n{brand}")
        key = next((k for k in overrides if k.lower() == brand.lower() or brand_of(k.upper()) == brand), None)
        if key:
            # handmatige keuze: lijst van {vanaf, bestand}
            periods = sorted(overrides[key], key=lambda p: p["vanaf"])
            candidates = []
            for i, p in enumerate(periods):
                c = file_info(p["bestand"])
                c.update({"from": p["vanaf"], "to": periods[i + 1]["vanaf"] - 1 if i + 1 < len(periods) else None, "index": i})
                candidates.append(c)
            print(f"  handmatige keuze uit {overrides_file.name}: {len(candidates)} bestand(en)")
        else:
            candidates = search_logos(brand)
            print(f"  {len(candidates)} kandidaten op Commons")
        time.sleep(0.5)

        chosen_for = {}
        for name, year in models:
            c = choose(candidates, year)
            if c is None:
                print(f"  {name} ({year}): geen logo gevonden")
                continue
            period = f"{c['from']}" if c["from"] else "x"
            chosen_for[name] = (year, c, f"{slug(brand)}-{period}.png")
            print(f"  {name} ({year}): {c['title']}  [{c['license']}]")

        if not args.droog:
            done = set()
            for name, (year, c, fname) in chosen_for.items():
                if fname not in done:
                    try:
                        img = to_standard_image(download(c["url"]), c["mime"])
                    except Exception as e:  # één mislukt logo mag de rest niet tegenhouden
                        print(f"  mislukt: {c['title']}: {e}")
                        continue
                    out_dir.mkdir(parents=True, exist_ok=True)
                    (out_dir / fname).write_bytes(png_bytes(img))
                    (out_dir / mono_name(fname)).write_bytes(png_bytes(to_mono(img)))
                    mapping["files"][fname] = {k: c[k] for k in ("title", "page", "license", "artist", "trademarked")}
                    done.add(fname)
                    time.sleep(0.3)
                if fname in done or (out_dir / fname).exists():
                    mapping["cars"][name] = fname
        report.append((brand, models, candidates, chosen_for))

    if not args.droog:
        out_dir.mkdir(parents=True, exist_ok=True)
        mapping_file.write_text(json.dumps(mapping, ensure_ascii=False, indent=1, sort_keys=True), encoding="utf-8")
        write_credits(out_dir, mapping)
        print(f"\n{len(mapping['cars'])} auto's hebben een logo; zie {mapping_file.relative_to(ROOT) if mapping_file.is_relative_to(ROOT) else mapping_file}")
        print(f"De app toont de versie '{mapping['stijl']}'.")
    write_overview(report, ROOT / "tools/logos-overzicht.html", out_dir, args.droog)
    print("Overzicht om te controleren: tools/logos-overzicht.html")


def mono_name(fname):
    return fname[:-4] + "-mono.png"


def write_credits(out_dir, mapping):
    lines = ["# Bronnen van de logo's", "",
             "Alle logo's komen van Wikimedia Commons. Ze zijn handelsmerken van hun eigenaars.", "",
             "| Bestand | Commons | Licentie | Maker | Handelsmerk |", "|---|---|---|---|---|"]
    for fname, f in sorted(mapping["files"].items()):
        lines.append(f"| {fname} | [{f['title']}]({f['page']}) | {f['license']} | {f['artist'][:60]} | {'ja' if f['trademarked'] else ''} |")
    (out_dir / "BRONNEN.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_overview(report, path, out_dir, dry):
    import os
    e = html.escape
    rel = Path(os.path.relpath(out_dir, path.parent)).as_posix()
    parts = ["<!doctype html><meta charset='utf-8'><title>Autosim logo's</title><style>",
             "body{font:14px system-ui,sans-serif;margin:24px;background:#f4f5f7;color:#1b1f24}",
             "section{background:#fff;border-radius:10px;padding:12px 16px;margin:0 0 16px}",
             ".row{display:flex;flex-wrap:wrap;gap:10px}.c{width:150px;font-size:11px;color:#5d6670}",
             ".c img{width:150px;height:100px;object-fit:contain;background:#fff;border:1px solid #ddd;border-radius:6px}",
             ".chosen img{border:3px solid #e8730c}h2{margin:0 0 6px;font-size:17px}ul{margin:4px 0 10px;padding-left:18px}",
             ".res{display:flex;flex-wrap:wrap;gap:14px;margin:0 0 12px}.res figure{margin:0;font-size:11px;color:#5d6670;text-align:center}",
             ".res img{width:96px;height:96px;border-radius:12px;display:block;margin:0 0 4px}",
             ".kleur img{background:#f2f3f5;border:1px solid #ddd}.mono img{background:#000}",
             "</style><h1>Logo's per merk</h1><p>Per merk eerst de omgezette logo's: in kleur zoals in de app (op een licht vlak) "
             "en monochroom (oranje op zwart). Daaronder alle kandidaten van Commons; oranje omlijnd is gekozen voor minstens "
             "één auto. Pas keuzes aan in <code>tools/logo-keuzes.json</code>; kies de versie voor de app met "
             "<code>--stijl kleur</code> of <code>--stijl mono</code>.</p>"]
    for brand, models, candidates, chosen_for in report:
        chosen_titles = {c["title"] for (_, c, _) in chosen_for.values()}
        parts.append(f"<section><h2>{e(brand)}</h2><ul>")
        for name, year in models:
            ch = chosen_for.get(name)
            parts.append(f"<li>{e(name)} ({year}): {e(ch[1]['title']) if ch else '<b>geen logo</b>'}</li>")
        parts.append("</ul>")
        if not dry:
            # resultaat: per gekozen logo de kleurversie (op het lichte vlak uit de app) en de monochrome versie
            parts.append("<div class='res'>")
            for fname in sorted({f for (_, _, f) in chosen_for.values()}):
                parts.append(f"<figure class='kleur'><img src='{e(rel)}/{e(fname)}' alt=''>{e(fname)}<br>kleur</figure>"
                             f"<figure class='mono'><img src='{e(rel)}/{e(mono_name(fname))}' alt=''>{e(fname)}<br>mono</figure>")
            parts.append("</div>")
        parts.append("<div class='row'>")
        for c in candidates:
            period = f"{c['from'] or '?'}-{c['to'] or ''}" if c["from"] else "geen jaartal"
            cls = "c chosen" if c["title"] in chosen_titles else "c"
            parts.append(f"<div class='{cls}'><a href='{e(c['page'] or '')}' target='_blank'><img src='{e(c['thumb'] or '')}' "
                         f"alt=''></a><br>{e(c['title'][5:])}<br>{e(period)} · {e(c['license'])}</div>")
        parts.append("</div></section>")
    path.write_text("".join(parts), encoding="utf-8")


if __name__ == "__main__":
    main()
