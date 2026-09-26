// Runs an `ng` command with the app version (from package.json) and the git
// commit baked in as the APP_VERSION / APP_COMMIT constants the footer shows.
//
//   node scripts/with-version.mjs build [ng options...]
import { execFileSync, spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';

const { version } = JSON.parse(readFileSync(new URL('../package.json', import.meta.url), 'utf8'));

let commit = 'unknown';
try {
  commit = execFileSync('git', ['rev-parse', '--short', 'HEAD'], { encoding: 'utf8' }).trim();
} catch {
  // Not a git checkout (e.g. a source archive); the footer says "unknown".
}

const args = [
  ...process.argv.slice(2),
  '--define', `APP_VERSION=${JSON.stringify(version)}`,
  '--define', `APP_COMMIT=${JSON.stringify(commit)}`
];
const result = spawnSync('ng', args, { stdio: 'inherit' });
process.exit(result.status ?? 1);
