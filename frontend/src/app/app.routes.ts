import { Routes } from '@angular/router';

/**
 * Route table.
 *
 * Every screen is loaded on demand, so signing in only downloads the login page rather
 * than the whole administration section as well. Paths are in Spanish because they are
 * part of the interface people read and share.
 */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'inicio' },
];
