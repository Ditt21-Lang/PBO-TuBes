#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVA_FX_CACHE="${ROOT_DIR}/.javafx-cache"

if ! command -v mvn >/dev/null 2>&1; then
    echo "Apache Maven (mvn) is required but not found in PATH." >&2
    echo "Install Maven and run ./build.sh once to download dependencies." >&2
    exit 1
fi

mkdir -p "$JAVA_FX_CACHE"

cd "$ROOT_DIR"
# Redirect JavaFX cache to a writable project folder to avoid permission errors.
JAVA_TOOL_OPTIONS="-Djavafx.cachedir=${JAVA_FX_CACHE}" mvn javafx:run
