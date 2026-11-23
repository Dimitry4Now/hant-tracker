import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Signed in, with nothing left to set up; otherwise to /login or /setup. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) {
    return router.createUrlTree(['/login']);
  }
  return auth.requiredStep() ? router.createUrlTree(['/setup']) : true;
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) {
    return router.createUrlTree(['/login']);
  }
  return auth.isAdmin() ? true : router.createUrlTree(['/dashboard']);
};

/** The setup page is only for a signed-in player who has a step to take. */
export const setupGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) {
    return router.createUrlTree(['/login']);
  }
  return auth.requiredStep() ? true : router.createUrlTree(['/dashboard']);
};
