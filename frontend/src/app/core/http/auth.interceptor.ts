import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { TokenStorage } from '../auth/token-storage';

/**
 * Attaches the bearer token to every call to our own API.
 *
 * The check on the URL matters: book covers are fetched from Open Library, and sending
 * our token to a third party would leak it.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = inject(TokenStorage).token();
  const isOwnApi = request.url.startsWith('/api/');

  if (!token || !isOwnApi) {
    return next(request);
  }
  return next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
