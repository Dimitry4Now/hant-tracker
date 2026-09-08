# Frontend

Angular 17 (standalone components, signals), generated with Angular CLI 17.3.

The CLI is a local dev dependency — there is no global `ng`, so use the npm
scripts or `npx ng ...`.

```
npm install
npm start          # dev server on http://localhost:4200
npm run build      # production bundle in dist/frontend
npm test           # unit tests via Karma (needs a local Chrome)
```

`npm start` proxies `/api` to `http://localhost:8080` (`proxy.conf.json`), so
run the API alongside it — see the root `README.md`.

## Layout

- `core/` — domain models, scoring rules, `HantDataService` (HTTP), `AuthService`
  (JWT), `authInterceptor`, route guards
- `shared/` — brand mark, theme toggle, `denToEur` and `signed` pipes
- `layout/` — signed-in shell (top nav, theme toggle)
- `features/` — public landing, auth, dashboard, analysis, admin screens

Screens follow the artboards in `design/`. Light and dark themes are token-based
in `src/styles.scss`; `ThemeService` stores the choice and applies it on first
paint.

Round points are computed in `core/scoring.ts` and sent to the API, which stores
them as given — the client owns the scoring rules.
