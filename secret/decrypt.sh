#!/usr/bin/env sh
# Unlock secret/*.env into the gitignored plaintext files each consumer reads.
# Usage: sh secret/decrypt.sh   (needs sops and the age private key)
set -eu
cd "$(dirname "$0")/.."
sops --decrypt --output backend/.env secret/backend.env
sops --decrypt --output app/secrets.properties secret/app.env
echo "wrote backend/.env and app/secrets.properties"
