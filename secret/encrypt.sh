#!/usr/bin/env sh
# Lock the plaintext secrets back into secret/*.env.
# Usage: sh secret/encrypt.sh   (needs sops and the age private key)
# A target is rewritten only when its decrypted content differs, so git stays quiet.
set -eu
cd "$(dirname "$0")/.."
tmp=$(mktemp)
trap 'rm -f "$tmp"' EXIT

lock() { # lock <plaintext> <encrypted>
  tr -d '\r' < "$1" > "$tmp" # Windows editors save CRLF; keep the ciphertext LF
  if [ -f "$2" ] && sops --decrypt "$2" | cmp -s - "$tmp"; then
    echo "unchanged  $2"
    return 0
  fi
  sops --encrypt --input-type dotenv --output-type dotenv \
    --filename-override "$2" --output "$2" "$tmp"
  echo "updated    $2"
}

lock backend/.env secret/backend.env
lock app/secrets.properties secret/app.env
