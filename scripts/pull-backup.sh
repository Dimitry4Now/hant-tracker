#!/bin/bash
#
# Copies the newest database backup from the production server to ./backups
# on this machine, so a copy exists off the server: a backup on the same disk
# doesn't survive losing the server. Run it now and then (or from cron on a
# machine that stays on). Same server options as scripts/deploy.sh:
#
#   --host HOST   DEPLOY_HOST   default: veldigital
#   --user USER   DEPLOY_USER   default: the local $USER
#
# The dumps are readable by root only, so sudo asks for the password once.

set -euo pipefail

host="${DEPLOY_HOST:-veldigital}"
user="${DEPLOY_USER:-$USER}"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --host) host="${2:?--host needs a value}"; shift ;;
        --user) user="${2:?--user needs a value}"; shift ;;
        -h|--help) sed -n '3,12p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) echo "Unknown option: $1" >&2; exit 1 ;;
    esac
    shift
done

repo="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
target="$user@$host"
mkdir -p "$repo/backups"
chmod 700 "$repo/backups"

# Stage the newest dump in the user's home, readable by them, then fetch it.
# This step shows sudo's password prompt, so its output isn't captured.
ssh -t "$target" '
    set -e
    newest="$(sudo find /var/backups -maxdepth 1 -name "hant-tracker-*.sql.gz" -printf "%T@ %p\n" | sort -rn | head -1 | cut -d" " -f2-)"
    [ -n "$newest" ] || { echo "No backups on the server yet" >&2; exit 1; }
    sudo install -m 600 -o "$USER" "$newest" "$HOME/.hant-tracker-pull.sql.gz"
    basename "$newest" > "$HOME/.hant-tracker-pull.name"
'
name="$(ssh "$target" 'cat "$HOME/.hant-tracker-pull.name"')"
scp -q "$target:.hant-tracker-pull.sql.gz" "$repo/backups/$name"
ssh "$target" 'rm -f "$HOME/.hant-tracker-pull.sql.gz" "$HOME/.hant-tracker-pull.name"'
gzip -t "$repo/backups/$name"
echo "Saved $repo/backups/$name ($(du -h "$repo/backups/$name" | cut -f1))"
