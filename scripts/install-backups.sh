#!/bin/bash
#
# Installs the nightly database backup on the production server: the backup
# script, its systemd service and a timer that runs it every night, keeping
# the newest 14 dumps in /var/backups. Run once from your machine; running it
# again updates the files. Same server options as scripts/deploy.sh:
#
#   --host HOST   DEPLOY_HOST   default: veldigital
#   --user USER   DEPLOY_USER   default: the local $USER
#
# It ends by taking a first backup, so you see it work.

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

dir="$(cd "$(dirname "$0")/backup" && pwd)"
target="$user@$host"

scp -q "$dir/hant-tracker-backup" "$dir/hant-tracker-backup.service" "$dir/hant-tracker-backup.timer" \
    "$target:/tmp/"
# sudo needs a terminal for the password, hence -t; it is asked once.
ssh -t "$target" '
    set -e
    sudo install -m 755 /tmp/hant-tracker-backup /usr/local/sbin/hant-tracker-backup
    sudo install -m 644 /tmp/hant-tracker-backup.service /tmp/hant-tracker-backup.timer /etc/systemd/system/
    rm -f /tmp/hant-tracker-backup /tmp/hant-tracker-backup.service /tmp/hant-tracker-backup.timer
    sudo systemctl daemon-reload
    sudo systemctl enable --now hant-tracker-backup.timer
    echo "Taking a first backup:"
    sudo systemctl start hant-tracker-backup.service
    sudo journalctl -u hant-tracker-backup -n 3 --no-pager -o cat
    systemctl list-timers hant-tracker-backup.timer --no-pager | head -2
'
