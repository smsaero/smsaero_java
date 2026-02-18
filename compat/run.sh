#!/usr/bin/env bash

set -e

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

VERSIONS=("11" "17" "21" "25")
IMAGES=("maven:3.9-eclipse-temurin-11" "maven:3.9-eclipse-temurin-17" "maven:3.9-eclipse-temurin-21" "maven:3.9-eclipse-temurin-25")

echo "=== SMS Aero Java ==="
echo ""

for i in "${!VERSIONS[@]}"; do
  JAVA_VER="${VERSIONS[$i]}"
  IMAGE="${IMAGES[$i]}"
  echo "[$((i+1))/${#VERSIONS[@]}] Java $JAVA_VER ($IMAGE)..."
  docker run --rm \
    -v "$ROOT:/project" \
    -w /project \
    "$IMAGE" \
    mvn clean test -q
  echo "  Java $JAVA_VER — success"
  echo ""
done

echo "=== Done ==="
