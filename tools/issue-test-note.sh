#!/usr/bin/env bash
# Local development issuer. Requires macOS /usr/bin/openssl and python3 only.
set -euo pipefail
usage() { echo 'Usage: tools/issue-test-note.sh init KEY.pem | issue KEY.pem OWNER_PUBLIC_KEY_BASE64 DENOMINATION OUTPUT.json'; exit 64; }
command -v openssl >/dev/null; command -v python3 >/dev/null || { echo 'python3 is required'; exit 1; }
case "${1:-}" in
  init)
    key="${2:-}"; [ -n "$key" ] || usage; [ ! -e "$key" ] || { echo "Refusing to overwrite $key"; exit 1; }
    umask 077; openssl ecparam -name prime256v1 -genkey -noout -out "$key"
    echo "Created $key. Keep it outside this repository and APK." ;;
  issue)
    key="${2:-}"; owner="${3:-}"; denomination="${4:-}"; output="${5:-}"; [ -f "$key" ] && [ -n "$owner" ] && [ -n "$denomination" ] && [ -n "$output" ] || usage
    [[ "$denomination" =~ ^[1-9][0-9]*$ ]] || { echo 'Denomination must be a positive integer'; exit 1; }
    [ ! -e "$output" ] || { echo "Refusing to overwrite $output"; exit 1; }
    issuer="$(openssl pkey -in "$key" -pubout -outform DER | base64 | tr -d '\n')"; serial="$(openssl rand -hex 16)"
    tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
    OWNER="$owner" ISSUER="$issuer" SERIAL="$serial" DENOMINATION="$denomination" PAYLOAD="$tmp/payload" python3 - <<'PY'
import os
def f(n,v): return f"{n}:{len(v.encode('utf-8'))}:{v}\n"
open(os.environ['PAYLOAD'],'wb').write((''.join([f('domain','offline-notes/issuance'),f('protocolVersion','1'),f('network','offline-notes-test-v1'),f('issuerId',os.environ['ISSUER']),f('serial',os.environ['SERIAL']),f('denomination',os.environ['DENOMINATION']),f('initialOwnerKey',os.environ['OWNER'])])).encode())
PY
    signature="$(openssl dgst -sha256 -sign "$key" "$tmp/payload" | base64 | tr -d '\n')"
    ISSUER="$issuer" SERIAL="$serial" OWNER="$owner" DENOMINATION="$denomination" SIGNATURE="$signature" OUTPUT="$output" python3 - <<'PY'
import json, os
with open(os.environ['OUTPUT'],'x') as out: json.dump({'issued':{'protocolVersion':1,'network':'offline-notes-test-v1','issuerId':os.environ['ISSUER'],'serial':os.environ['SERIAL'],'denomination':int(os.environ['DENOMINATION']),'initialOwnerKey':os.environ['OWNER'],'issuerSignature':os.environ['SIGNATURE']},'transfers':[]},out,separators=(',',':'))
PY
    echo "Wrote $output"
    echo "Explicitly trust this issuer public key in the app before importing: $issuer" ;;
  *) usage;;
esac
