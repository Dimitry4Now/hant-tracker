# Contributing

Thanks for looking at Hant Tracker. It is a small project built around one
regular card game, so the process is light — but a few conventions keep the
history readable.

## Getting set up

You need Node 20+ and a JDK 21. Nothing else is installed globally: the Angular
CLI is a local dev dependency and Gradle comes from the wrapper.

```
cd api && ./gradlew bootRun     # http://localhost:8080, dev profile
cd frontend && npm install && npm start   # http://localhost:4200
```

The dev profile seeds an in-memory H2 database with four players, a year of
weekly games and two pending sign-ups, so you have something to look at from
the first run. Sign in with `playera@example.com` (admin) and the password
`password`. The database is dropped on every restart.

`npm start` proxies `/api` to the API, so the browser only ever talks to its
own origin — the same arrangement as production.

## Before you open a pull request

```
cd frontend && npm run build && npm test
cd api && ./gradlew build
```

Both must pass. If a change touches scoring, add or update the tests in
`frontend/src/app/core/` — the scoring rules are the one part of the codebase
where a silent mistake corrupts real, already-played history.

## Style

- Match the code around you. The existing style is the style guide.
- Comments explain *why*, not *what*. Most code here needs none.
- Commits follow [Conventional Commits](https://www.conventionalcommits.org):
  `feat(frontend): …`, `fix(api): …`, `docs: …`, `refactor: …`, `chore: …`.
  The subject line stays under ~50 characters; the body explains the reasoning
  when it is not obvious from the diff.
- Branch off `develop`. `master` is the deployed branch.

## Scope of a pull request

One concern per pull request. A scoring fix and a layout tweak are two pull
requests, even when you found them in the same sitting — it keeps the history
useful when a number looks wrong six months from now.

## Design

Screens follow the artboards in `design/`. If you change a layout, say in the
pull request how it relates to the artboard: matching it, deliberately
diverging from it, or the artboard needs updating too.

## Reporting a bug

Open an issue with what you did, what you expected and what happened. For a
wrong number, include the game and round — the points a round awards are
defined in `frontend/src/app/core/scoring.ts`, and that file is usually the
fastest way to confirm whether the rules or the code is at fault.

Security problems do not go in issues — see [SECURITY.md](SECURITY.md).

## Licensing

The project is licensed under the GNU Affero General Public License v3.0. By
contributing you agree that your contribution is licensed under the same terms.
