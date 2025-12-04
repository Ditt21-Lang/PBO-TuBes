#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if ! command -v mvn >/dev/null 2>&1; then
    echo "Apache Maven (mvn) is required but not found in PATH." >&2
    exit 1
fi

cd "$ROOT_DIR"
mvn -B clean package