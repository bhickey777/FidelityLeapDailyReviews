#!/usr/bin/env bash

set -euo pipefail

AUTH_URL="${AUTH_URL:-http://localhost:4000/login}"
API_URL="${API_URL:-http://localhost:8080/accounts/1/orders}"

TOKEN="$(
  curl -sS -X POST "$AUTH_URL" \
    -H "Content-Type: application/json" \
    -d '{"username":"alice","password":"mission123"}' |
    sed -E 's/.*"token":"([^"]+)".*/\1/'
)"

if [[ -z "$TOKEN" || "$TOKEN" == '{"token":"' ]]; then
  echo "Failed to fetch auth token from $AUTH_URL" >&2
  exit 1
fi

echo "== Authenticated BUY request =="
curl -sS -X POST "$API_URL" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"ticker":"ULVR.L","instrumentType":"EQUITY","quantity":20,"price":40.0,"side":"BUY"}'

echo
echo
echo "== Unauthenticated request =="
curl -sS -i -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{"ticker":"ULVR.L","instrumentType":"EQUITY","quantity":10,"price":40.0,"side":"BUY"}'
