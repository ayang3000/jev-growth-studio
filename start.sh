#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [[ -f .env ]]; then
  set -a
  # .env is a local shell file. Only use one you trust.
  source .env
  set +a
fi

if [[ "${JEV_ENABLED:-false}" == "true" && ( -z "${JEV_API_KEY:-}" || "${JEV_API_KEY}" == "your-key" ) ]]; then
  echo "JEV_ENABLED=true requires JEV_API_KEY in .env or the environment." >&2
  exit 1
fi

if [[ "${AI_ENABLED:-false}" == "true" ]]; then
  if [[ -z "${OPENAI_API_KEY:-}" || "${OPENAI_API_KEY}" == "your-key" ]]; then
    echo "AI_ENABLED=true requires OPENAI_API_KEY in .env or the environment." >&2
    exit 1
  fi
  export SPRING_AI_CHAT_MODEL="${SPRING_AI_CHAT_MODEL:-openai}"
fi

exec ./gradlew bootRun "$@"
