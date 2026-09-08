# hant-tracker

## Structure

- `design/` — design canvas artboards (`.dc.html`) and reference mockups
- `frontend/` — Angular app
- `api/` — Spring Boot API (Java 21, Gradle, JPA/Hibernate, H2 in dev, PostgreSQL in prod)

## Running both

```
cd api && ./gradlew bootRun     # http://localhost:8080
cd frontend && npm start        # http://localhost:4200
```

`npm start` proxies `/api` to `http://localhost:8080` (`frontend/proxy.conf.json`),
so the browser only ever talks to its own origin.

In dev the API seeds an in-memory database with four regulars, a year of weekly
games and two pending sign-ups. Log in with any seeded email — `playera@example.com`
(admin), `playerb@example.com`, `playerc@example.com` — and the password
`password` (`app.seed.password`). `playerd@example.com` is seeded LOCKED on
purpose, so it is the one to test a rejected login with.

## Frontend

```
cd frontend
npm install
npm start          # http://localhost:4200
```

The Angular CLI is a local dev dependency — use the npm scripts (`npm start`,
`npm run build`, `npm test`) or `npx ng ...`; there is no global `ng`.

Screens follow the artboards in `design/`. `HantDataService` is a thin
`HttpClient` layer over the API; `AuthService` holds the JWT the API issues and
keeps it in `localStorage`, and `authInterceptor` attaches it to every `/api`
call and drops the session on a 401/403.

Layout:

- `core/` — models, scoring rules, data + auth services, HTTP interceptor, route guards
- `shared/` — brand mark, theme toggle, formatting pipes
- `layout/` — signed-in shell (top nav, theme toggle)
- `features/` — public landing, auth, dashboard, analysis, admin screens

Round points are computed in `core/scoring.ts` and sent to the API, which stores
them as given — the client owns the scoring rules.

## API

```
cd api
./gradlew bootRun                                  # dev profile: H2 + seed data
./gradlew bootRun --args='--spring.profiles.active=prod'   # PostgreSQL
```

The prod profile reads `DB_URL`, `DB_USER`, `DB_PASSWORD`, and — set these
before exposing it anywhere — `JWT_SECRET` and `CORS_ORIGINS`.

Auth is a stateless JWT: `POST /api/auth/login` returns a token plus the user,
and the token goes back as `Authorization: Bearer …`. Sign-ups (`POST
/api/auth/register`) become pending account requests that an admin approves.

Endpoints:

| Method | Path | Access |
| --- | --- | --- |
| POST | `/api/auth/login` | public |
| POST | `/api/auth/register` | public |
| GET | `/api/auth/me` | any user |
| GET | `/api/stats/public` | public |
| GET | `/api/stats/dashboard` | any user |
| GET | `/api/games`, `/api/games/{id}` | any user |
| POST | `/api/games`, `/api/games/{id}/rounds` | admin |
| PUT | `/api/games/{id}/money`, `/api/games/{id}/status` | admin |
| GET | `/api/users` | any user |
| POST | `/api/users` | admin |
| PATCH | `/api/users/{id}/status`, `/api/users/{id}/name` | admin |
| GET | `/api/account-requests` | admin |
| POST | `/api/account-requests/{id}/approve`, `/reject` | admin |

`/api/stats/*` returns the exact shapes in `frontend/src/app/core/models.ts`
(`DashboardStats`, `PublicStats`), so the screens render them as they arrive.
Dashboard numbers are the signed-in player's own; the leaderboard is all-time.
