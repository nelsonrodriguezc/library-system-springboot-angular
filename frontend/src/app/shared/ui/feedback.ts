import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { Icon, IconName } from './icon';

/**
 * Placeholder shown while data is on its way.
 *
 * A skeleton in the shape of the eventual content, rather than a spinner: the layout does
 * not jump when the data lands.
 */
@Component({
  selector: 'app-skeleton',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '',
  styles: `
    :host {
      display: block;
      background: linear-gradient(
        90deg,
        var(--surface-sunken) 25%,
        var(--border-subtle) 37%,
        var(--surface-sunken) 63%
      );
      background-size: 400% 100%;
      border-radius: var(--radius-sm);
      animation: shimmer 1.4s ease-in-out infinite;
      min-height: 14px;
    }

    @keyframes shimmer {
      0% {
        background-position: 100% 50%;
      }
      100% {
        background-position: 0 50%;
      }
    }

    @media (prefers-reduced-motion: reduce) {
      :host {
        animation: none;
      }
    }
  `,
})
export class Skeleton {}

/** Shown when a list legitimately has nothing in it. */
@Component({
  selector: 'app-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon],
  template: `
    <div class="empty">
      <span class="empty__icon"><app-icon [name]="icon()" [size]="26" /></span>
      <h3 class="empty__title">{{ title() }}</h3>
      @if (detail()) {
        <p class="empty__detail">{{ detail() }}</p>
      }
      <ng-content />
    </div>
  `,
  styles: `
    .empty {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      gap: var(--space-2);
      padding: var(--space-12) var(--space-6);
    }

    .empty__icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 52px;
      height: 52px;
      border-radius: 50%;
      background: var(--surface-sunken);
      color: var(--text-faint);
      margin-bottom: var(--space-2);
    }

    .empty__title {
      font-size: var(--text-md);
      font-weight: 650;
    }

    .empty__detail {
      color: var(--text-muted);
      max-width: 42ch;
    }
  `,
})
export class EmptyState {
  readonly icon = input<IconName>('inbox');
  readonly title = input.required<string>();
  readonly detail = input<string>();
}

/**
 * Shown when a request failed. Always offers a way to try again: an error the reader can
 * do nothing about is worse than no error at all.
 */
@Component({
  selector: 'app-error-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon],
  template: `
    <div class="error">
      <span class="error__icon"><app-icon name="alert-triangle" [size]="26" /></span>
      <h3 class="error__title">{{ title() }}</h3>
      <p class="error__detail">{{ detail() }}</p>
      <button type="button" class="btn btn--secondary btn--sm" (click)="retry.emit()">
        <app-icon name="refresh" [size]="15" />
        Reintentar
      </button>
    </div>
  `,
  styles: `
    .error {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      gap: var(--space-2);
      padding: var(--space-12) var(--space-6);
    }

    .error__icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 52px;
      height: 52px;
      border-radius: 50%;
      background: var(--state-danger-bg);
      color: var(--state-danger-fg);
      margin-bottom: var(--space-2);
    }

    .error__title {
      font-size: var(--text-md);
      font-weight: 650;
    }

    .error__detail {
      color: var(--text-muted);
      max-width: 46ch;
      margin-bottom: var(--space-3);
    }
  `,
})
export class ErrorState {
  readonly title = input('No pudimos cargar esta información');
  readonly detail = input('Revisa tu conexión e inténtalo nuevamente.');
  readonly retry = output<void>();
}
