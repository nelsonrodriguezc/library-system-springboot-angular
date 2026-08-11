import { Routes } from '@angular/router';
import { adminGuard, authGuard, guestGuard } from './core/auth/auth.guards';
import { Shell } from './layout/shell';

/**
 * Route table.
 *
 * Every screen is loaded on demand, so signing in only downloads the login page rather
 * than the administration section as well. Paths are in Spanish because they are part of
 * the interface people read and share.
 */
export const routes: Routes = [
  {
    path: 'ingresar',
    canActivate: [guestGuard],
    title: 'Iniciar sesión · Libris',
    loadComponent: () => import('./features/auth/login').then((m) => m.Login),
  },
  {
    path: 'registro',
    canActivate: [guestGuard],
    title: 'Crear cuenta · Libris',
    loadComponent: () => import('./features/auth/register').then((m) => m.Register),
  },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'inicio',
        title: 'Inicio · Libris',
        loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
      },
      // The more specific path is declared first: routes are matched in order.
      {
        path: 'catalogo/nuevo',
        canActivate: [adminGuard],
        title: 'Agregar libro · Libris',
        loadComponent: () => import('./features/catalog/add-book').then((m) => m.AddBook),
      },
      {
        path: 'catalogo',
        title: 'Catálogo · Libris',
        loadComponent: () => import('./features/catalog/catalog').then((m) => m.Catalog),
      },
      {
        path: 'admin',
        canActivate: [adminGuard],
        children: [
          { path: '', pathMatch: 'full', redirectTo: 'resumen' },
        ],
      },
    ],
  },
  { path: '**', redirectTo: 'inicio' },
];
