/**
 * Every request goes through a relative `/api` path: `npm start` proxies it to
 * the Spring Boot app (see `proxy.conf.json`), and in production the API is
 * served from the same origin as the built frontend.
 */
export const API_BASE = '/api';
