import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AuthStore } from '../../core/auth/auth.store';

/**
 * Landing screen. Filled in with the reader's own metrics, upcoming due dates and
 * recommendations once those endpoints are wired in; for now it confirms the session.
 */
@Component({
  selector: 'app-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <header class="page__header">
        <div>
          <h1 class="page__title">¡Bienvenida, {{ firstName() }}! 👋</h1>
          <p class="page__subtitle">Aquí tienes un resumen de la actividad de la biblioteca.</p>
        </div>
      </header>
    </div>
  `,
})
export class Dashboard {
  private readonly auth = inject(AuthStore);

  protected firstName(): string {
    return this.auth.displayName().split(/\s+/)[0] ?? '';
  }
}
