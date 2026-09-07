# hant-tracker

## Structure

- `design/` — design canvas artboards (`.dc.html`) and reference mockups
- `frontend/` — Angular app
- `api/` — Spring Boot API (Java 21, Gradle, PostgreSQL, JPA/Hibernate)

## Frontend

```
cd frontend
npm install
npm start          # http://localhost:4200
```

The Angular CLI is a local dev dependency — use the npm scripts (`npm start`,
`npm run build`, `npm test`) or `npx ng ...`; there is no global `ng`.

Screens follow the artboards in `design/`. All data currently comes from
`src/app/core/mock-data.ts` through `HantDataService`, which returns
Observables so its method bodies can be swapped for `HttpClient` calls once
the API exposes endpoints. `AuthService` is a placeholder too: any password
works for a known email.

Layout:

- `core/` — models, scoring rules, data + auth services, route guards
- `shared/` — brand mark, theme toggle, formatting pipes
- `layout/` — signed-in shell (top nav, theme toggle)
- `features/` — public landing, auth, dashboard, analysis, admin screens

## API

Requires a PostgreSQL database. Configure connection in
`api/src/main/resources/application.properties`.

```
cd api
./gradlew bootRun
```
