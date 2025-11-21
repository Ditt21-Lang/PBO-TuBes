#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MIGRATIONS_DIR="$ROOT_DIR/src/main/resources/db/migration"

if ! command -v psql >/dev/null 2>&1; then
    echo "psql command is required but not found in PATH." >&2
    exit 1
fi

if [ ! -d "$MIGRATIONS_DIR" ]; then
    echo "Migration directory not found: $MIGRATIONS_DIR" >&2
    exit 1
fi

if [ -f "$ROOT_DIR/.env" ]; then
    set -a
    # shellcheck disable=SC1090
    . "$ROOT_DIR/.env"
    set +a
fi

DB_URL_VALUE="${DB_URL:-jdbc:postgresql://localhost:5432/pomodone}"
DB_USER_VALUE="${DB_USER:-postgres}"
DB_PASSWORD_VALUE="${DB_PASSWORD:-}"

clean_url="$DB_URL_VALUE"
if [[ "$clean_url" == jdbc:* ]]; then
    clean_url="${clean_url#jdbc:}"
fi

scheme="postgresql"
rest="$clean_url"
if [[ "$clean_url" == *"://"* ]]; then
    scheme="${clean_url%%://*}"
    rest="${clean_url#*://}"
fi

if [[ "$rest" != *@* ]]; then
    PSQL_CONN="${scheme}://${DB_USER_VALUE}@${rest}"
else
    PSQL_CONN="${scheme}://${rest}"
fi

export PGPASSWORD="$DB_PASSWORD_VALUE"
trap 'unset PGPASSWORD' EXIT

found=0
while IFS= read -r migration; do
    [ -n "$migration" ] || continue
    found=1
    echo "Applying $(basename "$migration")..."
    psql "$PSQL_CONN" -v ON_ERROR_STOP=1 -f "$migration"
done < <(find "$MIGRATIONS_DIR" -type f -name '*.sql' | sort)

if [ "$found" -eq 0 ]; then
    echo "No SQL migration files found in $MIGRATIONS_DIR."
fi
