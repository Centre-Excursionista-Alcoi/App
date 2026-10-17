#!/usr/bin/env python3
"""
Builds the iOS String Catalog (iosApp/iosApp/Localizable.xcstrings) from the app's Compose resources
(composeApp/src/commonMain/composeResources/values*/strings.xml), which stay the source of truth: don't edit the
catalog by hand, run this after changing a string.

    python3 scripts/generate_xcstrings.py

Keys are the resources' names, so SwiftUI looks a string up with `Text("nav_home")` or `String(localized: "nav_home")`.
Android-style placeholders become Foundation's (`%1$s` -> `%1$@`, `%1$d` -> `%1$lld`), `<plurals>` become plural
variations, and Compose's escapes (`\\n`, `\\'`) are resolved.
"""
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RESOURCES = ROOT / "composeApp/src/commonMain/composeResources"
OUTPUT = ROOT / "iosApp/iosApp/Localizable.xcstrings"
SOURCE_LANGUAGE = "en"


def language_of(directory: Path) -> str:
    # `values` is the source language, `values-ca` is Catalan...
    return SOURCE_LANGUAGE if directory.name == "values" else directory.name.split("-", 1)[1]


def convert(text: str) -> str:
    text = text.replace("\\n", "\n").replace("\\'", "'").replace('\\"', '"').replace("\\\\", "\\")
    # %1$s -> %1$@ ; %s -> %@ ; %1$d -> %1$lld ; %d -> %lld (a `%%` is left alone)
    text = re.sub(r"%(\d+\$)?s", lambda m: f"%{m.group(1) or ''}@", text)
    text = re.sub(r"%(\d+\$)?d", lambda m: f"%{m.group(1) or ''}lld", text)
    return text


def unit(value: str) -> dict:
    return {"stringUnit": {"state": "translated", "value": convert(value)}}


def main() -> int:
    strings: dict[str, dict] = {}
    for directory in sorted(RESOURCES.glob("values*")):
        file = directory / "strings.xml"
        if not file.exists():
            continue
        language = language_of(directory)
        for element in ET.parse(file).getroot():
            name = element.get("name")
            if name is None:
                continue
            if element.tag == "string":
                localization = unit(element.text or "")
            elif element.tag == "plurals":
                localization = {"variations": {"plural": {
                    item.get("quantity"): unit(item.text or "") for item in element.findall("item")
                }}}
            else:
                continue
            strings.setdefault(name, {"extractionState": "manual", "localizations": {}})["localizations"][language] = localization

    catalog = {"sourceLanguage": SOURCE_LANGUAGE, "strings": dict(sorted(strings.items())), "version": "1.0"}
    OUTPUT.write_text(json.dumps(catalog, ensure_ascii=False, indent=2, sort_keys=False) + "\n", encoding="utf-8")
    print(f"Wrote {len(strings)} strings to {OUTPUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
