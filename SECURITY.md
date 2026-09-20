# Security Policy

## Supported versions

Hant Tracker is a single small deployment, not a released product. Only the
current `master` branch is supported; fixes land there and are deployed from
it.

## Reporting a vulnerability

Please do **not** open a public issue for a security problem.

Report it privately through GitHub's
[private vulnerability reporting](https://github.com/Dimitry4Now/hant-tracker/security/advisories/new)
on this repository, or by email to the maintainer listed on the GitHub profile
[@Dimitry4Now](https://github.com/Dimitry4Now).

Include what you found, how to reproduce it and what an attacker gets out of
it. A proof of concept against your own local instance is welcome; please do
not test against a live deployment you do not own.

Expect an acknowledgement within a few days. This is a hobby project run by one
person, so please allow a reasonable window for a fix before disclosing
publicly.

## What matters most here

The data is a group's game history and the money owed between them, plus player
accounts. The parts worth the most attention:

- **Authentication.** Auth is a stateless JWT signed with `JWT_SECRET`. Anyone
  who obtains that key can mint a token for any account, including an admin.
- **Authorisation.** Most write endpoints are admin-only (creating games,
  adding rounds, settling money, approving sign-ups). A route that lets an
  ordinary player reach one of those is a real finding.
- **Scoring integrity.** Round points are computed on the client and stored by
  the API as given — the API deliberately does not recompute them. That is a
  known design decision, not a vulnerability report; a signed-in player can
  already enter whatever number they like. What *is* a finding: writing rounds
  into a game without admin rights.

## Deployment hygiene

If you run your own instance, the defaults that must not survive into
production are `JWT_SECRET`, `DB_PASSWORD`, `DEFAULT_PASSWORD` and the
bootstrap `ADMIN_PASSWORD`. See `.env.example` and `DEPLOYMENT.md`; the
environment file holds credentials and should be `chmod 600`.
