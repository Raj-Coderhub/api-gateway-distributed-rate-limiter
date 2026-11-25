#!/usr/bin/env bash
# Usage: ./scripts/burst.sh [count] [api-key] [base-url]
# Fires N quick requests and prints the HTTP status of each.
COUNT=${1:-20}
KEY=${2:-}
BASE=${3:-http://localhost:8080}

for i in $(seq 1 "$COUNT"); do
  if [ -n "$KEY" ]; then
    curl -s -o /dev/null -w "request $i -> %{http_code}\n" -H "X-API-Key: $KEY" "$BASE/gateway/hello"
  else
    curl -s -o /dev/null -w "request $i -> %{http_code}\n" "$BASE/gateway/hello"
  fi
done
