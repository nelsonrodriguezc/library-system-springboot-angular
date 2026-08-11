import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from './auth.store';

/** Blocks anonymous access and remembers where the reader was going. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthStore);
  const router = inject(Router);

  return auth.isAuthenticated() || router.createUrlTree(['/ingresar'], { queryParams: { volverA: state.url } });
};

/**
 * Administration routes. The server enforces this too — this only keeps a librarian from
 * landing on a screen whose every request would come back 403.
 */
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthStore);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/ingresar']);
  }
  return auth.isAdmin() || router.createUrlTree(['/inicio']);
};

/** Keeps a signed-in reader away from the login screen. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthStore);
  const router = inject(Router);

  return !auth.isAuthenticated() || router.createUrlTree(['/inicio']);
};
