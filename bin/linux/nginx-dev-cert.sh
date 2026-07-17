#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
DOMAIN="${1:-localhost}"
CERT_DIR="$BASE_DIR/deploy/nginx/certs"
CERT_FILE="$CERT_DIR/fullchain.pem"
KEY_FILE="$CERT_DIR/privkey.pem"

mkdir -p "$CERT_DIR"

if [ -f "$CERT_FILE" ] && [ -f "$KEY_FILE" ]; then
  echo "Nginx development certificate already exists: $CERT_FILE"
  exit 0
fi

if ! command -v openssl >/dev/null 2>&1; then
  echo "openssl is required to generate a local development certificate" >&2
  exit 1
fi

openssl req \
  -x509 \
  -newkey rsa:2048 \
  -sha256 \
  -days 825 \
  -nodes \
  -keyout "$KEY_FILE" \
  -out "$CERT_FILE" \
  -subj "/CN=$DOMAIN" \
  -addext "subjectAltName=DNS:$DOMAIN,DNS:localhost,IP:127.0.0.1"

echo "Generated Nginx development certificate:"
echo "  certificate: $CERT_FILE"
echo "  private key: $KEY_FILE"
echo "Trust this certificate in your OS trust store before using the desktop client with strict TLS."
