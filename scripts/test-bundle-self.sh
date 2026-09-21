#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
# Run after the reactor install: this separate consumer resolves only the packaged bundle.
forgeit_version=$(python3 -c 'import xml.etree.ElementTree as E; print(E.parse("pom.xml").findtext("{http://maven.apache.org/POM/4.0.0}version"))')
exec ./mvnw -B -ntp -f tests/bundle-consumer/pom.xml "-Dforgeit.version=$forgeit_version" clean test "$@"
