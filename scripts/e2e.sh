#!/usr/bin/env bash
# End-to-end smoke test against the running Docker Compose stack.
# Usage: docker compose up -d --build --wait && bash scripts/e2e.sh
set -euo pipefail

GATEWAY=${GATEWAY:-http://localhost:8080}
KEYCLOAK=${KEYCLOAK:-http://localhost:8180}
pass=0

ok()   { echo "  PASS  $1"; pass=$((pass + 1)); }
fail() { echo "  FAIL  $1"; exit 1; }

# Pulls a top-level string field out of a JSON body without needing jq.
field() { sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p"; }

token() {
  curl -sf -X POST "$KEYCLOAK/realms/orders/protocol/openid-connect/token" \
    -d grant_type=password -d client_id=order-client -d username="$1" -d password="$1" | field access_token
}

status_of() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

place_order() {
  curl -sf -X POST "$GATEWAY/api/orders" -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -d "$2"
}

wait_for_status() {
  local id=$1 want=$2 got=""
  for _ in $(seq 1 30); do
    got=$(curl -sf "$GATEWAY/api/orders/$id" -H "Authorization: Bearer $ALICE" | field status)
    [ "$got" = "$want" ] && return 0
    sleep 1
  done
  echo "    order $id stayed $got, expected $want"
  return 1
}

echo "Getting tokens from Keycloak"
ALICE=$(token alice); BOB=$(token bob); ADMIN=$(token admin)
[ -n "$ALICE" ] && [ -n "$BOB" ] && [ -n "$ADMIN" ] && ok "issued JWTs for alice, bob, admin" || fail "token request"

echo "Security"
[ "$(status_of "$GATEWAY/api/orders")" = 401 ] && ok "no token -> 401 at the gateway" || fail "no token"
[ "$(status_of "$GATEWAY/api/orders" -H 'Authorization: Bearer forged.token.value')" = 401 ] \
  && ok "forged token -> 401" || fail "forged token"

echo "Happy path: order is confirmed through Kafka"
body=$(place_order "$ALICE" '{"lines":[{"sku":"KEYBOARD-01","quantity":2},{"sku":"MOUSE-01","quantity":1}]}')
ORDER=$(echo "$body" | field id)
[ "$(echo "$body" | field status)" = PENDING ] && ok "POST /api/orders -> 202 PENDING ($ORDER)" || fail "place order"
wait_for_status "$ORDER" CONFIRMED && ok "order-service -> Kafka -> inventory-service -> Kafka -> CONFIRMED" \
  || fail "confirmation"

echo "Rejection path: not enough stock"
body=$(place_order "$ALICE" '{"lines":[{"sku":"MONITOR-27","quantity":99}]}')
REJECTED=$(echo "$body" | field id)
wait_for_status "$REJECTED" REJECTED && ok "insufficient stock -> REJECTED" || fail "rejection"

echo "Authorization"
[ "$(status_of "$GATEWAY/api/orders/$ORDER" -H "Authorization: Bearer $BOB")" = 403 ] \
  && ok "bob cannot read alice's order -> 403" || fail "ownership check"
[ "$(status_of "$GATEWAY/api/orders/$ORDER" -H "Authorization: Bearer $ADMIN")" = 200 ] \
  && ok "admin can read any order -> 200" || fail "admin read"
[ "$(status_of -X POST "$GATEWAY/api/inventory/MONITOR-27/restock" -H "Authorization: Bearer $ALICE" \
    -H 'Content-Type: application/json' -d '{"name":"27-inch monitor","quantity":5}')" = 403 ] \
  && ok "customer cannot restock -> 403" || fail "restock as customer"
[ "$(status_of -X POST "$GATEWAY/api/inventory/MONITOR-27/restock" -H "Authorization: Bearer $ADMIN" \
    -H 'Content-Type: application/json' -d '{"name":"27-inch monitor","quantity":5}')" = 200 ] \
  && ok "admin restocks -> 200" || fail "restock as admin"

echo
echo "All $pass checks passed."
