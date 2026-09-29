// Captures the README screenshots from a running app: every screen below on
// desktop and mobile, in the light and the dark theme.
//
//   cd scripts/screenshots && npm install
//   node capture.mjs [out-dir] [screen,screen...]
//
// out-dir defaults to docs/screenshots; the screen list defaults to all of
// them. Run the dev profile (API + `npm start`) first — the shots rely on its
// seed data. Configure with:
//
//   BASE_URL   the frontend, default http://localhost:4200 (the API is reached
//              through its /api proxy)
//   CHROME     a Chrome/Chromium binary; without it, Playwright's own Chromium
//              (`npx playwright-core install chromium`)
import { chromium } from 'playwright-core';
import { fileURLToPath } from 'node:url';

const BASE = process.env.BASE_URL ?? 'http://localhost:4200';
const OUT = process.argv[2] ?? fileURLToPath(new URL('../../docs/screenshots', import.meta.url));
const only = process.argv[3]?.split(',');

async function api(path, init = {}) {
  const res = await fetch(BASE + path, init);
  if (!res.ok) {
    throw new Error(`${init.method ?? 'GET'} ${path}: ${res.status}`);
  }
  return res.json();
}

const login = await api('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email: 'playera@example.com', password: 'password' })
});
const session = JSON.stringify({ token: login.token, user: login.user });

// The newest finished game: in the seed data it has money and a session note,
// which the in-progress one at the top of the list does not.
const games = await api('/api/games', { headers: { Authorization: `Bearer ${login.token}` } });
const finished = games.findIndex((game) => !game.inProgress);
const game = games[finished];

const screens = [
  { name: 'public', path: '/', auth: false },
  { name: 'dashboard', path: '/dashboard' },
  { name: 'game-detail', path: `/admin/games/${game.id}` },
  {
    name: 'analysis',
    path: '/analysis',
    // The desktop page opens the newest game; the phone shows the list.
    prepare: async (page, device) => {
      if (device === 'desktop') {
        await page.locator('.game-row').nth(finished).click();
      }
    }
  }
].filter((screen) => !only || only.includes(screen.name));

const devices = {
  desktop: { viewport: { width: 1440, height: 900 }, deviceScaleFactor: 2 },
  mobile: { viewport: { width: 390, height: 844 }, deviceScaleFactor: 3, isMobile: true, hasTouch: true }
};

const browser = await chromium.launch({ executablePath: process.env.CHROME });
for (const [device, options] of Object.entries(devices)) {
  for (const theme of ['light', 'dark']) {
    const context = await browser.newContext({
      ...options,
      colorScheme: theme,
      reducedMotion: 'reduce',
      locale: 'en-GB'
    });
    for (const screen of screens) {
      // Same keys the app uses: ThemeService ('hant-theme'), AuthService ('hant-auth').
      await context.addInitScript(([theme, session, auth]) => {
        localStorage.setItem('hant-theme', theme);
        if (auth) {
          localStorage.setItem('hant-auth', session);
        } else {
          localStorage.removeItem('hant-auth');
        }
      }, [theme, session, screen.auth !== false]);
      const page = await context.newPage();
      await page.goto(BASE + screen.path, { waitUntil: 'networkidle' });
      await page.evaluate(() => document.fonts.ready);
      await screen.prepare?.(page, device);
      await page.waitForTimeout(1200);
      const file = `${OUT}/${screen.name}-${device}-${theme}.png`;
      await page.screenshot({ path: file });
      console.log(file);
      await page.close();
    }
    await context.close();
  }
}
await browser.close();
