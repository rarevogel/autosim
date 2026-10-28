#!/usr/bin/env python3
"""
Merklogo's voor Autosim, uit twee nette verzamelingen:

  1. Simple Icons (https://simpleicons.org): eenkleurige SVG-iconen, tekeningen onder CC0.
  2. car-logos-dataset (https://github.com/filippofilip95/car-logos-dataset): PNG's in kleur,
     voor de merken die niet in Simple Icons staan.

Per merk worden twee versies weggeschreven:

  tools/logos-origineel/<merk>.svg|png   het originele bestand, ongewijzigd
  assets/logos/<merk>-mono.png           gestandaardiseerd: 256 x 256 pixels, zwarte achtergrond,
                                         logo in het oranje van de app, gecentreerd met een vaste rand

Daarnaast:
  assets/logos/logos.json                welke auto welk logo krijgt (de app leest dit)
  assets/logos/BRONNEN.md                per merk de bron, de licentie en de link
  tools/merklogos-overzicht.html         origineel en monochrome versie naast elkaar, om te bekijken

Gebruik (vanuit de hoofdmap van de repo):

    pip install requests pillow cairosvg
    python3 tools/merklogos.py
    ./build.sh

Let op: Simple Icons en car-logos-dataset geven alleen het huidige logo van een merk, niet
de versie uit het bouwjaar van een auto. De logo's zijn handelsmerken van de fabrikanten;
assets/logos/ en tools/logos-origineel/ staan daarom in .gitignore.
"""

import argparse
import html
import io
import json
import os
import re
import sys
import tarfile
from pathlib import Path

try:
    import requests
    from PIL import Image, ImageChops
except ImportError:
    sys.exit("Installeer eerst de benodigde pakketten: pip install requests pillow cairosvg")

SIMPLE_ICONS_VERSION = "16.36.0"
SIMPLE_ICONS_TGZ = f"https://registry.npmjs.org/simple-icons/-/simple-icons-{SIMPLE_ICONS_VERSION}.tgz"
DATASET_RAW = "https://raw.githubusercontent.com/filippofilip95/car-logos-dataset/master/logos"
HEADERS = {"User-Agent": "AutosimMerklogos/1.0 (persoonlijk project)"}

SIZE = 256                      # gestandaardiseerd formaat: SIZE x SIZE pixels
MARGIN = 0.12                   # vaste lege rand rondom het logo, als deel van SIZE
ORANGE = (0xFF, 0x8A, 0x1F)     # het accentoranje van de app (App.ACCENT)
BLACK = (0, 0, 0)

ROOT = Path(__file__).resolve().parent.parent
SOURCES = [ROOT / "src/nl/rarevogel/autosim/core/Car.java", ROOT / "src/nl/rarevogel/autosim/core/CarsMore.java"]

# Merk -> (begin van de autonaam, slug in Simple Icons, slug in car-logos-dataset). None = staat er niet in.
BRANDS = {
    "Alfa Romeo":    (["ALFA ROMEO"], None, "alfa-romeo"),
    "Alpina":        (["BMW ALPINA"], None, "alpina"),
    "Alpine":        (["ALPINE"], None, "alpine"),
    "Aston Martin":  (["ASTON MARTIN"], "astonmartin", "aston-martin"),
    "Audi":          (["AUDI"], "audi", "audi"),
    "Austin-Healey": (["AUSTIN-HEALEY"], None, None),
    "Bentley":       (["BENTLEY"], "bentley", "bentley"),
    "BMW":           (["BMW"], "bmw", "bmw"),
    "Bugatti":       (["BUGATTI"], "bugatti", "bugatti"),
    "Chevrolet":     (["CHEVROLET", "CHEVROLETTE"], "chevrolet", "chevrolet"),
    "Citroën":       (["CITROEN"], "citroen", "citroen"),
    "Datsun":        (["DATSUN"], None, "datsun"),
    "De Tomaso":     (["DE TOMASO", "PANTERA"], None, "de-tomaso"),
    "DeLorean":      (["DELOREAN"], None, "dmc"),
    "Dodge":         (["DODGE"], None, "dodge"),
    "Donkervoort":   (["DONKERVOORT"], None, "donkervoort"),
    "Ferrari":       (["FERRARI"], "ferrari", "ferrari"),
    "Fiat":          (["FIAT"], "fiat", "fiat"),
    "Ford":          (["FORD"], "ford", "ford"),
    "Honda":         (["HONDA"], "honda", "honda"),
    "Jaguar":        (["JAGUAR"], None, "jaguar"),
    "Lamborghini":   (["LAMBORGHINI"], "lamborghini", "lamborghini"),
    "Lancia":        (["LANCIA"], None, "lancia"),
    "Lexus":         (["LEXUS"], None, "lexus"),
    "Lotus":         (["LOTUS"], None, "lotus"),
    "Maserati":      (["MASERATI"], "maserati", "maserati"),
    "Mazda":         (["MAZDA"], "mazda", "mazda"),
    "McLaren":       (["MCLAREN"], "mclaren", "mclaren"),
    "Mercedes-AMG":  (["MERCEDES-AMG"], None, "mercedes-amg"),
    "Mercedes-Benz": (["MERCEDES-BENZ", "MERCEDES"], None, "mercedes-benz"),
    "MG":            (["MG"], "mg", "mg"),
    "Mini":          (["MINI"], "mini", "mini"),
    "Mitsubishi":    (["MITSUBISHI"], "mitsubishi", "mitsubishi"),
    "Nissan":        (["NISSAN"], "nissan", "nissan"),
    "Opel":          (["OPEL"], "opel", "opel"),
    "Peugeot":       (["PEUGEOT"], "peugeot", "peugeot"),
    "Plymouth":      (["PLYMOUTH"], None, "plymouth"),
    "Pontiac":       (["PONTIAC"], None, "pontiac"),
    "Porsche":       (["PORSCHE"], "porsche", "porsche"),
    "Renault":       (["RENAULT"], "renault", "renault"),
    "Saab":          (["SAAB"], None, "saab"),
    "Shelby":        (["SHELBY"], None, None),
    "Subaru":        (["SUBARU"], "subaru", "subaru"),
    "Toyota":        (["TOYOTA"], "toyota", "toyota"),
    "Volkswagen":    (["VOLKSWAGEN", "VW"], "volkswagen", "volkswagen"),
}


# ------------------------------------------------------------------ auto's uit de broncode

def read_cars():
    """Naam -> bouwjaar van alle auto's, uit Car.java en CarsMore.java."""
    cars = {}
    original = SOURCES[0].read_text(encoding="utf-8")
    for name in re.findall(r'add\("(.{30})', original):
        cars[name.strip()] = None
    for name, year in re.findall(r'setYear\("([^"]+)",\s*(\d{4})\)', original):
        cars[name] = int(year)
    for name, year in re.findall(r'a\("([^"]+)",\s*"[^"]*"\)\.year\((\d{4})\)', SOURCES[1].read_text(encoding="utf-8")):
        cars[name] = int(year)
    return cars


def brand_of(car_name):
    """Het merk bij een autonaam; het langste passende begin wint (BMW ALPINA voor BMW)."""
    best, best_len = None, -1
    for brand, (prefixes, _, _) in BRANDS.items():
        for p in prefixes:
            if (car_name == p or car_name.startswith(p + " ")) and len(p) > best_len:
                best, best_len = brand, len(p)
    return best


def slug(brand):
    return re.sub(r"[^a-z0-9]+", "-", brand.lower().replace("ë", "e")).strip("-")


# ------------------------------------------------------------------ bronnen ophalen

def get(url):
    r = requests.get(url, headers=HEADERS, timeout=120)
    r.raise_for_status()
    return r.content


def load_simple_icons():
    """Downloadt het Simple Icons-pakket één keer en geeft (svg per slug, gegevens per slug)."""
    print(f"Simple Icons {SIMPLE_ICONS_VERSION} ophalen...")
    tgz = tarfile.open(fileobj=io.BytesIO(get(SIMPLE_ICONS_TGZ)), mode="r:gz")
    wanted = {si for (_, si, _) in BRANDS.values() if si}
    svgs, meta = {}, {}
    for member in tgz.getmembers():
        name = member.name
        if name == "package/data/simple-icons.json":
            for icon in json.load(tgz.extractfile(member)):
                if icon["slug"] in wanted:
                    meta[icon["slug"]] = icon
        m = re.fullmatch(r"package/icons/([a-z0-9]+)\.svg", name)
        if m and m.group(1) in wanted:
            svgs[m.group(1)] = tgz.extractfile(member).read()
    return svgs, meta


# ------------------------------------------------------------------ standaardiseren

def render_svg(svg_bytes, fill=None, width=SIZE * 4):
    try:
        import cairosvg
    except ImportError:
        sys.exit("Voor de SVG's van Simple Icons is cairosvg nodig: pip install cairosvg")
    if fill:
        # Simple Icons-SVG's hebben één pad zonder eigen kleur; de kleur op <svg> erft het pad
        svg_bytes = svg_bytes.replace(b"<svg ", b'<svg fill="#%02X%02X%02X" ' % fill, 1)
    return Image.open(io.BytesIO(cairosvg.svg2png(bytestring=svg_bytes, output_width=width))).convert("RGBA")


def fit_on_black(strength):
    """
    Maakt het standaardformaat: 'strength' (grijswaardenmasker, 255 = vol logo) wordt bijgesneden,
    geschaald tot binnen de vaste rand en als oranje op een zwart vierkant van SIZE pixels gezet.
    """
    box = strength.getbbox()
    if box:
        strength = strength.crop(box)
    inner = int(SIZE * (1 - 2 * MARGIN))
    scale = min(inner / strength.width, inner / strength.height)
    strength = strength.resize((max(1, round(strength.width * scale)), max(1, round(strength.height * scale))), Image.LANCZOS)
    out = Image.new("RGB", (SIZE, SIZE), BLACK)
    out.paste(Image.new("RGB", strength.size, ORANGE), ((SIZE - strength.width) // 2, (SIZE - strength.height) // 2), strength)
    return out


def mono_from_svg(svg_bytes):
    """Simple Icons: het pad is de vorm, dus de transparantie is precies het logo."""
    return fit_on_black(render_svg(svg_bytes, fill=ORANGE).getchannel("A"))


def mono_from_png(png_bytes):
    """
    car-logos-dataset (kleur): donkere en verzadigde delen worden oranje, lichte en grijze delen
    (witte letters, zilverglans) zwart. Een logo in (vrijwel) één kleur wordt helemaal oranje.
    """
    img = Image.open(io.BytesIO(png_bytes)).convert("RGBA")
    if img.width > 2048 or img.height > 2048:
        img.thumbnail((2048, 2048), Image.LANCZOS)
    alpha = img.getchannel("A")
    lum = img.convert("L")
    visible = sorted(l for l, a in zip(lum.tobytes(), alpha.tobytes()) if a > 128)
    if not visible:
        return fit_on_black(alpha)
    lo, hi = visible[len(visible) // 50], visible[-1 - len(visible) // 50]
    if hi - lo < 40:
        strength = alpha
    else:
        inv = lum.point(lambda l: max(0, min(255, round((hi - l) * 255 / (hi - lo)))))
        # harder contrast: tussentinten (glans, schaduw) worden vol oranje of zwart, alleen de
        # randen blijven zacht; zo oogt het als een egaal icoon in plaats van een bruinige foto
        inv = inv.point(lambda v: max(0, min(255, round((v - 80) * 255 / 90))))
        strength = ImageChops.multiply(inv, alpha)
    return fit_on_black(strength)


# ------------------------------------------------------------------ hoofdprogramma

def main():
    ap = argparse.ArgumentParser(description="Merklogo's voor Autosim uit Simple Icons en car-logos-dataset.")
    ap.add_argument("--uit", default=str(ROOT / "assets/logos"), help="map voor de logo's die in de app gaan")
    ap.add_argument("--origineel", default=str(ROOT / "tools/logos-origineel"), help="map voor de originele bestanden")
    args = ap.parse_args()
    out_dir, orig_dir = Path(args.uit), Path(args.origineel)
    out_dir.mkdir(parents=True, exist_ok=True)
    orig_dir.mkdir(parents=True, exist_ok=True)

    cars = read_cars()
    used = {}
    for name in cars:
        b = brand_of(name)
        if b is None:
            print(f"Onbekend merk voor {name}; voeg het toe aan BRANDS.")
        else:
            used.setdefault(b, []).append(name)

    svgs, si_meta = load_simple_icons()
    dataset_index = None
    results = {}   # merk -> dict met bron, bestanden, licentie

    for brand in sorted(used):
        _, si_slug, ds_slug = BRANDS[brand]
        s = slug(brand)
        mono_file = f"{s}-mono.png"
        if si_slug and si_slug in svgs:
            svg = svgs[si_slug]
            (orig_dir / f"{s}.svg").write_bytes(svg)
            (out_dir / mono_file).write_bytes(png_bytes(mono_from_svg(svg)))
            meta = si_meta.get(si_slug, {})
            lic = meta.get("license", {}).get("type", "CC0-1.0") if isinstance(meta.get("license"), dict) else "CC0-1.0"
            results[brand] = {"bron": "Simple Icons", "origineel": f"{s}.svg", "mono": mono_file, "licentie": lic,
                              "link": f"https://simpleicons.org/?q={si_slug}", "herkomst": meta.get("source", "")}
            print(f"{brand:15} Simple Icons ({si_slug})")
        elif ds_slug:
            if dataset_index is None:
                print("car-logos-dataset: index ophalen...")
                dataset_index = {e["slug"]: e for e in json.loads(get(f"{DATASET_RAW}/data.json"))}
            entry = dataset_index.get(ds_slug)
            if entry is None:
                print(f"{brand:15} niet gevonden in car-logos-dataset ({ds_slug})")
                continue
            # het originele bestand; is dat beschadigd (dat komt voor), dan de geoptimaliseerde versie
            mono = None
            for kind in ("original", "optimized"):
                try:
                    raw = get(entry["image"][kind])
                    mono = mono_from_png(raw)
                    break
                except Exception as e:
                    print(f"{brand:15} {kind} onbruikbaar ({e}), probeer de volgende versie")
            if mono is None:
                continue
            ext = os.path.splitext(entry["image"][kind])[1].lower() or ".png"
            (orig_dir / f"{s}{ext}").write_bytes(raw)
            (out_dir / mono_file).write_bytes(png_bytes(mono))
            results[brand] = {"bron": "car-logos-dataset", "origineel": f"{s}{ext}", "mono": mono_file,
                              "licentie": "handelsmerk van de eigenaar (verzameling: MIT)",
                              "link": f"https://github.com/filippofilip95/car-logos-dataset/blob/master/logos/{kind}/{ds_slug}{ext}",
                              "herkomst": entry["image"].get("source", "")}
            print(f"{brand:15} car-logos-dataset ({ds_slug})")
        else:
            print(f"{brand:15} geen logo beschikbaar in beide bronnen")

    # logos.json: de app zet zelf '-mono' achter de naam als de stijl 'mono' is
    mapping = {"stijl": "mono", "cars": {}, "files": {}}
    for brand, names in used.items():
        if brand in results:
            for n in names:
                mapping["cars"][n] = f"{slug(brand)}.png"
            r = results[brand]
            mapping["files"][r["mono"]] = {"merk": brand, "bron": r["bron"], "licentie": r["licentie"], "link": r["link"]}
    (out_dir / "logos.json").write_text(json.dumps(mapping, ensure_ascii=False, indent=1, sort_keys=True), encoding="utf-8")

    lines = ["# Bronnen van de merklogo's", "",
             "De logo's zijn handelsmerken van hun eigenaars. De tekeningen van Simple Icons vallen onder CC0;",
             "car-logos-dataset heeft een MIT-licentie voor de verzameling, niet voor de logo's zelf.", "",
             "| Merk | Bron | Licentie | Link | Oorspronkelijke herkomst |", "|---|---|---|---|---|"]
    for brand, r in sorted(results.items()):
        lines.append(f"| {brand} | {r['bron']} | {r['licentie']} | [link]({r['link']}) | {r['herkomst']} |")
    missing = sorted(b for b in used if b not in results)
    if missing:
        lines += ["", "Geen logo in beide bronnen: " + ", ".join(missing) + "."]
    (out_dir / "BRONNEN.md").write_text("\n".join(lines) + "\n", encoding="utf-8")

    write_overview(results, used, missing, out_dir, orig_dir, ROOT / "tools/merklogos-overzicht.html")
    print(f"\n{len(results)} merken met een logo, {len(mapping['cars'])} auto's. Ontbreekt: {', '.join(missing) or 'geen'}.")
    print("Bekijk tools/merklogos-overzicht.html en bouw daarna de app met ./build.sh.")


def png_bytes(img):
    buf = io.BytesIO()
    img.save(buf, "PNG", optimize=True)
    return buf.getvalue()


def write_overview(results, used, missing, out_dir, orig_dir, path):
    e = html.escape
    r_out = Path(os.path.relpath(out_dir, path.parent)).as_posix()
    r_orig = Path(os.path.relpath(orig_dir, path.parent)).as_posix()
    parts = ["<!doctype html><meta charset='utf-8'><title>Autosim merklogo's</title><style>",
             "body{font:14px system-ui,sans-serif;margin:24px;background:#16191e;color:#eceff3}",
             ".grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(250px,1fr));gap:14px}",
             ".m{background:#22272e;border-radius:12px;padding:12px}.m h2{margin:0 0 8px;font-size:15px}",
             ".pair{display:flex;gap:10px}.pair figure{margin:0;font-size:11px;color:#9aa3ae;text-align:center}",
             ".pair img{width:108px;height:108px;object-fit:contain;border-radius:10px;display:block;margin:0 0 4px}",
             ".o img{background:#f2f3f5}.src{font-size:11px;color:#9aa3ae;margin-top:6px}",
             "</style><h1>Merklogo's</h1><p>Links het originele bestand, rechts de gestandaardiseerde versie "
             "(256 × 256, oranje op zwart) zoals de app hem toont.</p><div class='grid'>"]
    for brand, r in sorted(results.items()):
        parts.append(f"<div class='m'><h2>{e(brand)}</h2><div class='pair'>"
                     f"<figure class='o'><img src='{e(r_orig)}/{e(r['origineel'])}' alt=''>origineel</figure>"
                     f"<figure><img src='{e(r_out)}/{e(r['mono'])}' alt=''>mono</figure></div>"
                     f"<div class='src'>{e(r['bron'])} · {len(used[brand])} auto('s)</div></div>")
    parts.append("</div>")
    if missing:
        parts.append(f"<p>Geen logo beschikbaar: {e(', '.join(missing))}.</p>")
    path.write_text("".join(parts), encoding="utf-8")


if __name__ == "__main__":
    main()
