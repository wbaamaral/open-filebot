#!/usr/bin/env python3
"""
Generate the Open FileBot SVG icon set from Tabler Icons.

    python3 tools/icons/generate-icons.py

Reads tools/icons/icons.tsv and writes source/net/filebot/resources/svg/<name>.svg.
Colors are written as placeholders (see resources/svg/colors.properties) that are
replaced by design tokens at runtime. Downloaded icons are cached in
tools/icons/.cache so that the set can be regenerated offline.
"""

import os
import re
import sys
import urllib.request

TABLER_VERSION = "3.48.0"
TABLER_URL = "https://cdn.jsdelivr.net/npm/@tabler/icons@{version}/icons/outline/{name}.svg"

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
MAPPING = os.path.join(ROOT, "tools", "icons", "icons.tsv")
CACHE = os.path.join(ROOT, "tools", "icons", ".cache", TABLER_VERSION)
OUTPUT = os.path.join(ROOT, "source", "net", "filebot", "resources", "svg")
COLORS = os.path.join(OUTPUT, "colors.properties")
TOKENS = os.path.join(ROOT, "source", "net", "filebot", "theme", "FlatLaf.properties")


def read_properties(path):
    values = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                values[key.strip()] = value.strip()
    return values


def tabler(name):
    path = os.path.join(CACHE, name + ".svg")
    if not os.path.exists(path):
        os.makedirs(CACHE, exist_ok=True)
        url = TABLER_URL.format(version=TABLER_VERSION, name=name)
        with urllib.request.urlopen(url) as response:
            data = response.read().decode("utf-8")
        with open(path, "w", encoding="utf-8") as f:
            f.write(data)
    with open(path, encoding="utf-8") as f:
        return f.read()


def inner(svg):
    # drawing elements without the outer <svg> element and the invisible bounding box path
    body = re.search(r"<svg[^>]*>(.*)</svg>", svg, re.S).group(1)
    body = re.sub(r'<path stroke="none" d="M0 0h24v24H0z" fill="none"\s*/>', "", body)
    return body.strip()


def icon(body, color, size):
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 24 24" '
        f'fill="none" stroke="{color}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">'
        f"{body}</svg>\n"
    )


def tile(body, background, glyph, size, radius):
    # colored rounded square with the glyph centered at 2/3 of the tile size
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 48 48">'
        f'<rect x="2" y="2" width="44" height="44" rx="{radius}" fill="{background}"/>'
        f'<g transform="translate(8 8) scale(1.3333)" fill="none" stroke="{glyph}" stroke-width="1.75" '
        f'stroke-linecap="round" stroke-linejoin="round">{body}</g></svg>\n'
    )


def main():
    colors = {role: value.split(",")[0].strip() for role, value in read_properties(COLORS).items()}
    radius = int(read_properties(TOKENS)["FileBot.radius.lg"])

    count = 0
    with open(MAPPING, encoding="utf-8") as f:
        for line in f:
            if not line.strip() or line.startswith("#"):
                continue
            name, tabler_name, role, size = line.rstrip("\n").split("\t")
            body = inner(tabler(tabler_name))
            if role.startswith("tile:"):
                svg = tile(body, colors["tile." + role[5:]], colors["onTile"], int(size), radius)
            else:
                svg = icon(body, colors[role], int(size))
            with open(os.path.join(OUTPUT, name + ".svg"), "w", encoding="utf-8") as out:
                out.write(svg)
            count += 1

    print(f"Generated {count} icons from Tabler Icons {TABLER_VERSION} into {os.path.relpath(OUTPUT, ROOT)}")


if __name__ == "__main__":
    sys.exit(main())
