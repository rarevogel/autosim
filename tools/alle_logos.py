#!/usr/bin/env python3
"""
Alle beschikbare autologo's voor het merkenoverzicht in Autosim.

De lijst met merken komt uit car-logos-dataset (https://github.com/filippofilip95/car-logos-dataset,
387 automerken). Per merk:
  1. staat het merk in Simple Icons (https://simpleicons.org), dan wordt die SVG gebruikt;
  2. anders het logo uit car-logos-dataset.

Uitvoer:
  tools/merken-origineel/<merk>.svg|png   het originele bestand, ongewijzigd
  assets/merken/<merk>.png                gestileerd: 256 x 256, oranje op zwart (zoals in de app)
  assets/merken/merken.json               naam, bestand en bron per merk (de app leest dit)
  assets/merken/BRONNEN.md                bron en licentie per merk
  tools/merken-overzicht.html             origineel en gestileerde versie naast elkaar

Gebruik (vanuit de hoofdmap van de repo):

    pip install requests pillow cairosvg
    python3 tools/alle_logos.py
    ./build.sh

De omzetting naar oranje op zwart is dezelfde als in tools/merklogos.py (dat bestand moet
ernaast staan). De logo's zijn handelsmerken van hun eigenaars; de uitvoermappen staan in
.gitignore.
"""

import html
import io
import json
import os
import re
import sys
import tarfile
import time
import unicodedata
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from merklogos import (DATASET_RAW, SIMPLE_ICONS_TGZ, SIMPLE_ICONS_VERSION,  # noqa: E402
                       get, mono_from_png, mono_from_svg, png_bytes)

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "assets/merken"
ORIG = ROOT / "tools/merken-origineel"
OVERVIEW = ROOT / "tools/merken-overzicht.html"

# Simple Icons-iconen met dezelfde naam als een automerk, maar van een ander bedrijf
NOT_CARS = {
    "eagle",      # Eagle, software
    "proton",     # Proton, e-mail en vpn
    "saturn",     # Saturn, Duitse elektronicawinkel
}


def norm(s):
    """Vergelijkbare naam: zonder accenten, leestekens en hoofdletters (Škoda -> skoda)."""
    s = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode()
    return re.sub(r"[^a-z0-9]", "", s.lower())


def load_simple_icons_all():
    """Alle Simple Icons: naam (genormaliseerd) -> (gegevens, svg)."""
    print(f"Simple Icons {SIMPLE_ICONS_VERSION} ophalen...")
    tgz = tarfile.open(fileobj=io.BytesIO(get(SIMPLE_ICONS_TGZ)), mode="r:gz")
    data, svgs = [], {}
    for member in tgz.getmembers():
        if member.name == "package/data/simple-icons.json":
            data = json.load(tgz.extractfile(member))
        m = re.fullmatch(r"package/icons/([a-z0-9]+)\.svg", member.name)
        if m:
            svgs[m.group(1)] = tgz.extractfile(member).read()
    by_name = {}
    for icon in data:
        if icon["slug"] in NOT_CARS or icon["slug"] not in svgs:
            continue
        names = [icon["title"]]
        aliases = icon.get("aliases")
        if isinstance(aliases, dict):
            names += aliases.get("aka", [])
        for n in names:
            by_name.setdefault(norm(n), (icon, svgs[icon["slug"]]))
    return by_name


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    ORIG.mkdir(parents=True, exist_ok=True)
    si = load_simple_icons_all()
    print("car-logos-dataset: lijst met merken ophalen...")
    dataset = json.loads(get(f"{DATASET_RAW}/data.json"))

    brands, failed = [], []
    for n, entry in enumerate(sorted(dataset, key=lambda e: norm(e["name"]))):
        name, slug = entry["name"], entry["slug"]
        hit = si.get(norm(name))
        try:
            if hit:
                icon, svg = hit
                (ORIG / f"{slug}.svg").write_bytes(svg)
                (OUT / f"{slug}.png").write_bytes(png_bytes(mono_from_svg(svg)))
                lic = icon["license"].get("type", "CC0-1.0") if isinstance(icon.get("license"), dict) else "CC0-1.0"
                brands.append({"naam": name, "bestand": f"{slug}.png", "origineel": f"{slug}.svg",
                               "bron": "Simple Icons", "licentie": lic, "herkomst": icon.get("source", "")})
            else:
                img, used = None, None
                for kind in ("original", "optimized"):     # sommige originelen zijn beschadigd
                    try:
                        raw = get(entry["image"][kind])
                        img, used = mono_from_png(raw), kind
                        break
                    except Exception:
                        continue
                if img is None:
                    raise RuntimeError("geen bruikbaar bestand")
                ext = os.path.splitext(entry["image"][used])[1].lower() or ".png"
                (ORIG / f"{slug}{ext}").write_bytes(raw)
                (OUT / f"{slug}.png").write_bytes(png_bytes(img))
                brands.append({"naam": name, "bestand": f"{slug}.png", "origineel": f"{slug}{ext}",
                               "bron": "car-logos-dataset", "licentie": "handelsmerk van de eigenaar (verzameling: MIT)",
                               "herkomst": entry["image"].get("source", "")})
                time.sleep(0.1)
        except Exception as e:
            failed.append(name)
            print(f"  mislukt: {name}: {e}")
            continue
        if (n + 1) % 25 == 0:
            print(f"  {n + 1}/{len(dataset)}")

    (OUT / "merken.json").write_text(json.dumps({"merken": [{k: b[k] for k in ("naam", "bestand", "bron")} for b in brands]},
                                                ensure_ascii=False, indent=1), encoding="utf-8")
    lines = ["# Bronnen van de merklogo's", "",
             "De logo's zijn handelsmerken van hun eigenaars. De tekeningen van Simple Icons vallen onder CC0;",
             "car-logos-dataset heeft een MIT-licentie voor de verzameling, niet voor de logo's zelf.", "",
             "| Merk | Bron | Licentie | Oorspronkelijke herkomst |", "|---|---|---|---|"]
    lines += [f"| {b['naam']} | {b['bron']} | {b['licentie']} | {b['herkomst']} |" for b in brands]
    (OUT / "BRONNEN.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    write_overview(brands, failed)
    n_si = sum(b["bron"] == "Simple Icons" for b in brands)
    print(f"\n{len(brands)} merken: {n_si} uit Simple Icons, {len(brands) - n_si} uit car-logos-dataset."
          + (f" Mislukt: {', '.join(failed)}." if failed else ""))
    print(f"Bekijk {OVERVIEW.relative_to(ROOT)} en bouw daarna de app met ./build.sh.")


def write_overview(brands, failed):
    e = html.escape
    r_out = Path(os.path.relpath(OUT, OVERVIEW.parent)).as_posix()
    r_orig = Path(os.path.relpath(ORIG, OVERVIEW.parent)).as_posix()
    n_si = sum(b["bron"] == "Simple Icons" for b in brands)
    parts = ["<!doctype html><html lang='nl'><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>",
             "<title>Autosim merklogo's</title><style>",
             "body{font:14px system-ui,sans-serif;margin:0;padding:24px max(16px,3vw);background:#16191e;color:#eceff3}",
             "h1{margin:0 0 4px}p{color:#9aa3ae;margin:0 0 16px}",
             "input{font:inherit;padding:8px 12px;border-radius:8px;border:1px solid #3a414b;background:#22272e;color:#eceff3;width:min(320px,100%);margin:0 0 18px}",
             ".grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:12px}",
             ".m{background:#22272e;border-radius:12px;padding:10px 12px}.m h2{margin:0 0 8px;font-size:14px}",
             ".pair{display:flex;gap:8px}.pair img{width:104px;height:104px;object-fit:contain;border-radius:10px;display:block}",
             ".o{background:#f2f3f5}.s{background:#000}.src{font-size:11px;color:#9aa3ae;margin-top:6px}",
             f"</style><h1>Merklogo's</h1><p>{len(brands)} merken: {n_si} uit Simple Icons, {len(brands) - n_si} uit "
             "car-logos-dataset. Links het origineel, rechts de gestileerde versie (256 × 256, oranje op zwart).</p>",
             "<input id='q' type='search' placeholder='Zoek een merk' aria-label='Zoek een merk'><div class='grid' id='g'>"]
    for b in brands:
        parts.append(f"<div class='m' data-n='{e(b['naam'].lower())}'><h2>{e(b['naam'])}</h2><div class='pair'>"
                     f"<img class='o' loading='lazy' src='{e(r_orig)}/{e(b['origineel'])}' alt='origineel'>"
                     f"<img class='s' loading='lazy' src='{e(r_out)}/{e(b['bestand'])}' alt='gestileerd'></div>"
                     f"<div class='src'>{e(b['bron'])}</div></div>")
    parts.append("</div>")
    if failed:
        parts.append(f"<p>Mislukt: {e(', '.join(failed))}.</p>")
    parts.append("<script>document.getElementById('q').addEventListener('input',function(){var q=this.value.toLowerCase();"
                 "document.querySelectorAll('.m').forEach(function(m){m.hidden=q&&m.dataset.n.indexOf(q)<0;});});</script>")
    OVERVIEW.write_text("".join(parts), encoding="utf-8")


if __name__ == "__main__":
    main()
