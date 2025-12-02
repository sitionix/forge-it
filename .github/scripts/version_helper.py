#!/usr/bin/env python3
"""Utility helpers for Maven version management.

Parses the root ``pom.xml`` version, derives the release version (without
``-SNAPSHOT``), and computes the next patch snapshot version. Designed to work
inside CI without depending on Maven's network access.
"""
from __future__ import annotations

import argparse
import sys
import xml.etree.ElementTree as ET

NAMESPACE = {"m": "http://maven.apache.org/POM/4.0.0"}


def read_version(pom_path: str) -> str:
    try:
        tree = ET.parse(pom_path)
    except ET.ParseError as exc:  # pragma: no cover - defensive guard
        raise SystemExit(f"Unable to parse {pom_path}: {exc}") from exc

    version_el = tree.getroot().find("m:version", NAMESPACE)
    if version_el is None or not version_el.text:
        raise SystemExit(f"No <version> element found in {pom_path}")
    return version_el.text.strip()


def compute_versions(raw_version: str) -> tuple[str, str]:
    base_version = raw_version.removesuffix("-SNAPSHOT")
    parts = base_version.split(".")
    if len(parts) != 3 or not all(part.isdigit() for part in parts):
        raise SystemExit(
            "Project version must follow semantic format X.Y.Z or X.Y.Z-SNAPSHOT"
        )

    major, minor, patch = map(int, parts)
    release_version = f"{major}.{minor}.{patch}"
    next_version = f"{major}.{minor}.{patch + 1}-SNAPSHOT"
    return release_version, next_version


def export_env(release_version: str, next_version: str) -> str:
    return f"RELEASE_VERSION={release_version}\nNEXT_VERSION={next_version}\n"


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Compute release/next Maven versions")
    parser.add_argument(
        "action",
        choices=["print", "export"],
        nargs="?",
        default="print",
        help="Print versions to stdout or emit shell-ready env vars",
    )
    parser.add_argument(
        "--pom",
        dest="pom_path",
        default="pom.xml",
        help="Path to the root pom.xml (defaults to ./pom.xml)",
    )

    args = parser.parse_args(argv)
    raw_version = read_version(args.pom_path)
    release_version, next_version = compute_versions(raw_version)

    if args.action == "print":
        print(f"release={release_version}\nnext={next_version}")
    else:
        sys.stdout.write(export_env(release_version, next_version))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
