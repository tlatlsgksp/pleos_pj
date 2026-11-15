#!/usr/bin/env sh
# Unlock secret/*.env into the gitignored plaintext files each consumer reads.
# Usage: sh secret/decrypt.sh   (needs sops and the age private key)
set -eu
cd "$(dirname "$0")/.."
sops --decrypt --output apps/backend/.env secret/backend.env
sops --decrypt --output apps/android/secrets.properties secret/android.env
echo "wrote apps/backend/.env and apps/android/secrets.properties"
