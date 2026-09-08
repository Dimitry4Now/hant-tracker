import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { API_BASE } from './api.config';
import { AuthService } from './auth.service';

/**
 * Attaches the bearer token to API calls, and drops the session when the API
 * rejects it — an expired or revoked token should send the player to /login
 * instead of leaving broken screens behind.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const token = auth.token;
  const authorized =
    token && request.url.startsWith(API_BASE)
      ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : request;

  return next(authorized).pipe(
    catchError((error: unknown) => {
      const status = error instanceof HttpErrorResponse ? error.status : 0;
      if ((status === 401 || status === 403) && auth.isLoggedIn()) {
        auth.logout();
        void router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
