// Replaced at build time by scripts/with-version.mjs. A bare `ng build` or
// `ng serve` leaves them undefined, hence the typeof guards.
declare const APP_VERSION: string | undefined;
declare const APP_COMMIT: string | undefined;

export const appVersion = typeof APP_VERSION === 'string' ? APP_VERSION : 'dev';
export const appCommit = typeof APP_COMMIT === 'string' ? APP_COMMIT : 'local';
