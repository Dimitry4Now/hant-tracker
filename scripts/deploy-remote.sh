#!/usr/bin/env bash
#
# The server half of deploy.sh, which uploads and runs it — not meant to be
# run by hand. Installs whatever deploy.sh left in /tmp.
#
#   deploy-remote.sh <backend:true|false> <frontend:true|false> <api-port> <root> <service>

set -euo pipefail

backend=$1
frontend=$2
port=$3
root=$4
service=$5

health="http://localhost:$port/api/stats/public"
database=hant_tracker
backups=/var/backups
keep_backups=10

if $backend; then
    # The new jar may migrate the schema on start (Flyway), so dump the
    # database first. A failed dump stops the deploy before anything changes.
    backup="$backups/hant-tracker-$(date +%Y%m%d-%H%M%S)-predeploy.sql.gz"
    echo "Backing up the database to $backup"
    sudo -u postgres pg_dump "$database" | gzip | sudo tee "$backup" > /dev/null
    sudo chmod 600 "$backup"
    # Only the newest few pre-deploy dumps are kept; nightly backups are untouched.
    sudo find "$backups" -maxdepth 1 -name 'hant-tracker-*-predeploy.sql.gz' -printf '%T@ %p\n' \
        | sort -rn | tail -n +$((keep_backups + 1)) | cut -d' ' -f2- | xargs -r sudo rm -f

    if sudo test -f "$root/server/hant-tracker.jar"; then
        echo "Keeping the current jar as hant-tracker.jar.prev"
        sudo cp "$root/server/hant-tracker.jar" "$root/server/hant-tracker.jar.prev"
    fi
    sudo install -o hant -g hant -m 640 /tmp/hant-tracker.jar "$root/server/hant-tracker.jar"
    sudo systemctl restart "$service"

    echo -n "Waiting for the API on port $port "
    up=false
    for _ in $(seq 60); do
        if curl -fs -o /dev/null "$health"; then
            up=true
            break
        fi
        echo -n "."
        sleep 2
    done

    if ! $up; then
        echo " no answer after 2 minutes. Last log lines:"
        sudo journalctl -u "$service" -n 40 --no-pager
        echo
        echo "To roll back:"
        echo "  sudo cp $root/server/hant-tracker.jar.prev $root/server/hant-tracker.jar && sudo systemctl restart $service"
        echo "The database as it was before this deploy is in $backup"
        exit 1
    fi
    echo " up"
    rm -f /tmp/hant-tracker.jar
fi

if $frontend; then
    echo "Replacing the client files"
    sudo rsync -a --delete --chown=www-data:www-data /tmp/hant-client/ "$root/client/"
fi

echo "Done."
