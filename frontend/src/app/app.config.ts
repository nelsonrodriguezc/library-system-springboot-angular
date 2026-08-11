import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { registerLocaleData } from '@angular/common';
import localeEsCl from '@angular/common/locales/es-CL';
import { routes } from './app.routes';
import { authInterceptor } from './core/http/auth.interceptor';
import { errorInterceptor } from './core/http/error.interceptor';

// Dates and numbers are shown the way a Chilean reader expects them.
registerLocaleData(localeEsCl);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    { provide: LOCALE_ID, useValue: 'es-CL' },
    provideRouter(
      routes,
      // Query parameters map straight onto component inputs, which keeps the catalogue
      // filters shareable as a URL instead of trapped in component state.
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'top', anchorScrolling: 'enabled' }),
    ),
    // Order matters: the token is attached first, then failures are translated for the
    // reader on the way back out.
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
  ],
};
