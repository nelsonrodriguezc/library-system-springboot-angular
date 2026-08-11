import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { AuthStore } from '../auth/auth.store';
import { ToastService } from '../ui/toast.service';
import { ApiError } from '../models/api.models';

/** Endpoints that report their own failures inline, where a toast would be noise. */
const HANDLED_IN_PLACE = [/\/api\/auth\/(login|register)$/, /\/api\/books\/lookup\//];

/**
 * Turns a failed request into something the reader can see, exactly once.
 *
 * The backend always answers with the same error shape, so the message it chose is the
 * one shown; the generic fallbacks only cover the cases where the response never made it
 * back at all.
 */
export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const toasts = inject(ToastService);
  const auth = inject(AuthStore);

  return next(request).pipe(
    catchError((response: HttpErrorResponse) => {
      if (response.status === 401 && auth.isAuthenticated()) {
        auth.sessionExpired();
        toasts.info('Tu sesión expiró', 'Vuelve a iniciar sesión para continuar.');
        return throwError(() => response);
      }

      if (!HANDLED_IN_PLACE.some((pattern) => pattern.test(request.url))) {
        toasts.error(titleFor(response), describe(response));
      }
      return throwError(() => response);
    }),
  );
};

function titleFor(response: HttpErrorResponse): string {
  if (response.status === 0) {
    return 'No pudimos conectar con el servidor';
  }
  if (response.status === 403) {
    return 'No tienes permisos para esta acción';
  }
  if (response.status >= 500) {
    return 'El servidor tuvo un problema';
  }
  return 'No pudimos completar la acción';
}

function describe(response: HttpErrorResponse): string | undefined {
  if (response.status === 0) {
    return 'Revisa tu conexión e inténtalo de nuevo.';
  }
  const body = response.error as ApiError | null;
  if (body?.fieldErrors) {
    return Object.values(body.fieldErrors).join(' ');
  }
  return body?.message ?? undefined;
}

/** Pulls the backend's message out of a failed response, for inline error display. */
export function apiErrorMessage(response: unknown, fallback: string): string {
  const error = (response as HttpErrorResponse)?.error as ApiError | undefined;
  if (error?.fieldErrors) {
    return Object.values(error.fieldErrors).join(' ');
  }
  return error?.message ?? fallback;
}
