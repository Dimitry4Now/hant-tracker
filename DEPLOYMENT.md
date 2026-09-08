# Deployment

First deploy of hant-tracker on a production server, and how to update it after.

Assumes a single Debian or Ubuntu host with systemd, nginx and PostgreSQL, and
a domain pointing at it. Adjust package names for other distributions; nothing
below depends on nginx in particular — any reverse proxy that serves static
files and forwards `/api` will do.

## Layout on the server

Everything lives under `/opt/hant-tracker`:

```
/opt/hant-tracker/
├── server/
│   ├── hant-tracker.jar        the Spring Boot fat jar
│   ├── hant-tracker.env        secrets and settings, read by systemd
│   └── logs/                   rolling application logs
└── client/                     the built Angular app (index.html, assets, chunks)
```

The service runs as a dedicated unprivileged `hant` user, which only needs write
access to `server/logs`.

## 1. Build the artifacts

On a build machine or CI, from a clean checkout:

```
cd api      && ./gradlew clean bootJar     # build/libs/api-0.0.1-SNAPSHOT.jar
cd frontend && npm ci && npm run build     # dist/frontend/browser/
```

The Angular build writes to `dist/frontend/browser` — that directory's
*contents* are what goes into `/opt/hant-tracker/client`.

## 2. Prepare the host

```
sudo apt update
sudo apt install openjdk-21-jre-headless postgresql nginx

sudo useradd --system --home /opt/hant-tracker/server --shell /usr/sbin/nologin hant
sudo mkdir -p /opt/hant-tracker/server/logs /opt/hant-tracker/client
sudo chown -R hant:hant /opt/hant-tracker/server
sudo chown -R www-data:www-data /opt/hant-tracker/client
```

## 3. Create the database

```
sudo -u postgres psql <<'SQL'
CREATE USER hant_tracker WITH PASSWORD 'a-strong-password';
CREATE DATABASE hant_tracker OWNER hant_tracker;
SQL
```

The app creates and updates its own tables on start (`ddl-auto=update`), so
there is no migration step for the first deploy.

## 4. Write the environment file

Generate a JWT secret first — it must be at least 32 bytes, and changing it
later logs everyone out:

```
openssl rand -base64 48
```

`/opt/hant-tracker/server/hant-tracker.env`:

```
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080

DB_URL=jdbc:postgresql://localhost:5432/hant_tracker
DB_USER=hant_tracker
DB_PASSWORD=a-strong-password

JWT_SECRET=the-openssl-output-from-above
LOG_DIR=/opt/hant-tracker/server/logs

# Password given to players an admin adds by hand.
DEFAULT_PASSWORD=change-me-on-first-login

# Only for the very first start — see step 8.
ADMIN_EMAIL=you@example.com
ADMIN_PASSWORD=a-strong-password
ADMIN_NAME=Your Name
```

It holds credentials, so lock it down:

```
sudo chown hant:hant /opt/hant-tracker/server/hant-tracker.env
sudo chmod 600 /opt/hant-tracker/server/hant-tracker.env
```

`CORS_ORIGINS` is not needed while nginx serves the client and the API under one
domain — the browser only ever makes same-origin requests. Set it only if the
frontend is hosted somewhere else.

## 5. Upload the artifacts

From the build machine:

```
scp api/build/libs/api-0.0.1-SNAPSHOT.jar you@server:/tmp/hant-tracker.jar
rsync -av --delete frontend/dist/frontend/browser/ you@server:/tmp/hant-client/
```

On the server:

```
sudo install -o hant -g hant -m 640 /tmp/hant-tracker.jar /opt/hant-tracker/server/hant-tracker.jar
sudo rsync -av --delete --chown=www-data:www-data /tmp/hant-client/ /opt/hant-tracker/client/
```

## 6. The systemd service

`/etc/systemd/system/hant-tracker.service`:

```ini
[Unit]
Description=hant-tracker API
After=network-online.target postgresql.service
Wants=network-online.target postgresql.service

[Service]
Type=simple
User=hant
Group=hant
WorkingDirectory=/opt/hant-tracker/server
EnvironmentFile=/opt/hant-tracker/server/hant-tracker.env
ExecStart=/usr/bin/java -XX:MaxRAMPercentage=75 -jar /opt/hant-tracker/server/hant-tracker.jar
Restart=on-failure
RestartSec=5
# Spring Boot exits with 143 on SIGTERM; that is a clean stop, not a failure.
SuccessExitStatus=143

NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/opt/hant-tracker/server/logs

[Install]
WantedBy=multi-user.target
```

```
sudo systemctl daemon-reload
sudo systemctl enable --now hant-tracker
systemctl status hant-tracker
curl -s localhost:8080/api/stats/public | head -c 200
```

## 7. nginx

`/etc/nginx/sites-available/hant-tracker`:

```nginx
server {
    listen 80;
    server_name hant.example.com;

    root /opt/hant-tracker/client;
    index index.html;

    # Angular owns the routing — unknown paths render the app, not a 404.
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Hashed filenames, so they can be cached hard. index.html must not be.
    location ~* \.(js|css|woff2?|png|svg|ico)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```
sudo ln -s /etc/nginx/sites-available/hant-tracker /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d hant.example.com          # rewrites the block for TLS
```

Leave only 22, 80 and 443 open; the API listens on localhost and should never be
reachable directly.

## 8. First login, then drop the bootstrap credentials

On an empty database the API creates one ADMIN account from `ADMIN_EMAIL` /
`ADMIN_PASSWORD` and logs `Created the bootstrap admin account for …`. Log in at
`https://hant.example.com/login`, then remove those three `ADMIN_*` lines from
the env file and restart:

```
sudo systemctl restart hant-tracker
```

They do nothing once a user exists, but there is no reason to keep a password in
a file. From then on, new players arrive through the sign-up form and an admin
approves them under **Admin → Requests**, or are added directly under
**Admin → Users** (they get `DEFAULT_PASSWORD`).

## 9. Logs

The service logs to `/opt/hant-tracker/server/logs/hant-tracker.log`, rolling at
10 MB into gzipped files named `hant-tracker.<date>.<n>.log.gz`, keeping 30 days
and at most 1 GB. The settings live in `api/src/main/resources/application-prod.properties`
(`logging.logback.rollingpolicy.*`) and the directory follows `LOG_DIR`.

```
tail -f /opt/hant-tracker/server/logs/hant-tracker.log
journalctl -u hant-tracker -f          # the same lines, via stdout
```

No `logrotate` entry is needed — Logback does the rotation itself.

## 10. Updating

```
# build the new artifacts, upload them as in step 5, then:
sudo cp /opt/hant-tracker/server/hant-tracker.jar /opt/hant-tracker/server/hant-tracker.jar.prev
sudo install -o hant -g hant -m 640 /tmp/hant-tracker.jar /opt/hant-tracker/server/hant-tracker.jar
sudo systemctl restart hant-tracker
sudo rsync -av --delete --chown=www-data:www-data /tmp/hant-client/ /opt/hant-tracker/client/
```

The client is static files, so replacing them needs no nginx reload. Schema
changes are applied on start by `ddl-auto=update`; it adds tables and columns
but never drops them, so a release that removes a field leaves the old column
behind for a later manual cleanup.

To roll back, put `hant-tracker.jar.prev` back and restart — but check whether
the newer version changed the schema first.

## 11. Backups

The database holds everything worth keeping; the jar and the client are
rebuildable.

```
sudo -u postgres pg_dump hant_tracker | gzip > /var/backups/hant-tracker-$(date +%F).sql.gz
```

Run it from cron nightly and copy the dumps off the host.

## Checks after any deploy

```
systemctl is-active hant-tracker
curl -s https://hant.example.com/api/stats/public | head -c 200   # public, no token
curl -s -o /dev/null -w '%{http_code}\n' https://hant.example.com/api/stats/dashboard  # expect 401
```

Then log in through the browser and open the dashboard — that exercises the
token, the proxy and the database in one go.
