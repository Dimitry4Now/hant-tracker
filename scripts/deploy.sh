#!/usr/bin/env bash
#
# Builds hant-tracker and deploys it to the production server, following the
# layout in DEPLOYMENT.md. Run from anywhere inside the repo.
#
#   scripts/deploy.sh             backend and frontend
#   scripts/deploy.sh --be        backend only
#   scripts/deploy.sh --fe        frontend only
#
# Where it deploys to comes from flags, then environment variables, then the
# defaults below:
#
#   --host HOST   DEPLOY_HOST   default: veldigital
#   --user USER   DEPLOY_USER   default: the local $USER
#   --port PORT   DEPLOY_PORT   default: 8099 (the API port on the server,
#                                         used for the health check)
#
# The server needs rsync, and the user needs sudo there. Everything that needs
# sudo runs in one ssh session at the end, so the sudo password is asked once.

set -euo pipefail

REMOTE_ROOT=/opt/hant-tracker
SERVICE=hant-tracker

host="${DEPLOY_HOST:-veldigital}"
user="${DEPLOY_USER:-$USER}"
port="${DEPLOY_PORT:-8099}"
backend=false
frontend=false

usage() {
    sed -n '3,19p' "$0" | sed 's/^# \{0,1\}//'
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --be) backend=true ;;
        --fe) frontend=true ;;
        --host) host="${2:?--host needs a value}"; shift ;;
        --user) user="${2:?--user needs a value}"; shift ;;
        --port) port="${2:?--port needs a value}"; shift ;;
        -h|--help) usage; exit 0 ;;
        *) echo "Unknown option: $1" >&2; usage >&2; exit 1 ;;
    esac
    shift
done

# Neither flag means both.
if ! $backend && ! $frontend; then
    backend=true
    frontend=true
fi

repo="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
target="$user@$host"
step() { printf '\n==> %s\n' "$*"; }

parts=()
$backend && parts+=(backend)
$frontend && parts+=(frontend)
step "Deploying ${parts[*]} from $(git -C "$repo" rev-parse --abbrev-ref HEAD)@$(git -C "$repo" rev-parse --short HEAD) to $target"
if [[ -n "$(git -C "$repo" status --porcelain)" ]]; then
    echo "Warning: the working tree has uncommitted changes; they will be deployed too."
fi

# --- build ----------------------------------------------------------------

if $backend; then
    step "Building the API"
    (cd "$repo/api" && ./gradlew clean bootJar)
    # bootJar is the fat jar; the -plain one from the jar task cannot run on its own.
    jar="$(find "$repo/api/build/libs" -name '*.jar' ! -name '*-plain.jar' | head -n 1)"
    [[ -n "$jar" ]] || { echo "No jar found in api/build/libs" >&2; exit 1; }
fi

if $frontend; then
    step "Building the frontend"
    (cd "$repo/frontend" && npm ci && npm run build)
    client="$repo/frontend/dist/frontend/browser"
    [[ -f "$client/index.html" ]] || { echo "No index.html in $client" >&2; exit 1; }
fi

# --- upload ---------------------------------------------------------------

if $backend; then
    step "Uploading the jar"
    scp "$jar" "$target:/tmp/hant-tracker.jar"
fi

if $frontend; then
    step "Uploading the client"
    rsync -az --delete "$client/" "$target:/tmp/hant-client/"
fi

# --- install --------------------------------------------------------------

step "Installing on $host"
# sudo needs a terminal to ask for the password, so the install steps travel
# as a file rather than on stdin, and ssh gets -t.
scp -q "$repo/scripts/deploy-remote.sh" "$target:/tmp/hant-deploy-remote.sh"
ssh -t "$target" "bash /tmp/hant-deploy-remote.sh $backend $frontend $port $REMOTE_ROOT $SERVICE; status=\$?; rm -f /tmp/hant-deploy-remote.sh; exit \$status"
