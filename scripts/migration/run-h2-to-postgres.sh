#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MIGRATION_DIR="$REPO_ROOT/scripts/migration"

if [[ -f "$REPO_ROOT/.env" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$REPO_ROOT/.env"
  set +a
fi

PG_URL="${PG_URL:-${SPRING_DATASOURCE_URL:-}}"
PG_USER="${PG_USER:-${SPRING_DATASOURCE_USERNAME:-}}"
PG_PASSWORD="${PG_PASSWORD:-${SPRING_DATASOURCE_PASSWORD:-}}"

if [[ -z "${PG_URL}" || -z "${PG_USER}" || -z "${PG_PASSWORD}" ]]; then
  echo "PG_URL, PG_USER, and PG_PASSWORD must be set (either directly or via .env)." >&2
  exit 1
fi

export PG_URL PG_USER PG_PASSWORD

H2_JAR=$(ls -1 "$HOME/.m2/repository/com/h2database/h2"/*/h2-*.jar 2>/dev/null | sort | tail -n 1 || true)
PG_JAR=$(ls -1 "$HOME/.m2/repository/org/postgresql/postgresql"/*/postgresql-*.jar 2>/dev/null | sort | tail -n 1 || true)

if [[ -z "$H2_JAR" ]]; then
  echo "H2 jar not found in ~/.m2. Run 'mvn dependency:get -Dartifact=com.h2database:h2:2.2.224' first." >&2
  exit 1
fi
if [[ -z "$PG_JAR" ]]; then
  echo "PostgreSQL driver jar not found in ~/.m2. Build the backend once or run 'mvn dependency:go-offline'." >&2
  exit 1
fi

pushd "$MIGRATION_DIR" >/dev/null
javac -cp "$H2_JAR:$PG_JAR" H2ToPostgresMigrator.java
java -cp "$H2_JAR:$PG_JAR:." H2ToPostgresMigrator
popd >/dev/null
