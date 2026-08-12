import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Icon, IconName } from './icon';
import { ToastService, ToastTone } from '../../core/ui/toast.service';

const TONE_ICON: Record<ToastTone, IconName> = {
  success: 'check',
  error: 'alert-circle',
  info: 'info',
};

@Component({
  selector: 'app-toast-container',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon],
  template: `
    <div class="stack" aria-live="polite" aria-atomic="false">
      @for (toast of toasts.toasts(); track toast.id) {
        <div class="toast" [class]="'toast--' + toast.tone" role="status">
          <span class="toast__icon"><app-icon [name]="iconFor(toast.tone)" [size]="17" /></span>
          <div class="toast__body">
            <strong class="toast__title">{{ toast.title }}</strong>
            @if (toast.detail) {
              <span class="toast__detail">{{ toast.detail }}</span>
            }
          </div>
          <button type="button" class="toast__close" aria-label="Cerrar" (click)="toasts.dismiss(toast.id)">
            <app-icon name="close" [size]="15" />
          </button>
        </div>
      }
    </div>
  `,
  styles: `
    .stack {
      position: fixed;
      right: var(--space-6);
      bottom: var(--space-6);
      z-index: 100;
      display: flex;
      flex-direction: column;
      gap: var(--space-3);
      width: min(380px, calc(100vw - 32px));
    }

    .toast {
      display: flex;
      align-items: flex-start;
      gap: var(--space-3);
      padding: var(--space-4);
      background: var(--surface-card);
      border: 1px solid var(--border-subtle);
      border-left: 3px solid var(--text-faint);
      border-radius: var(--radius-md);
      box-shadow: var(--shadow-lg);
      animation: slide-in var(--duration-base) var(--ease-out);
    }

    .toast--success { border-left-color: var(--state-success-fg); }
    .toast--error { border-left-color: var(--state-danger-fg); }
    .toast--info { border-left-color: var(--brand-indigo); }

    .toast__icon {
      display: flex;
      margin-top: 1px;
    }

    .toast--success .toast__icon { color: var(--state-success-fg); }
    .toast--error .toast__icon { color: var(--state-danger-fg); }
    .toast--info .toast__icon { color: var(--brand-indigo); }

    .toast__body {
      display: flex;
      flex-direction: column;
      gap: 2px;
      flex: 1;
      min-width: 0;
    }

    .toast__title {
      font-size: var(--text-base);
      font-weight: 600;
    }

    .toast__detail {
      font-size: var(--text-sm);
      color: var(--text-muted);
      overflow-wrap: anywhere;
    }

    .toast__close {
      display: flex;
      background: none;
      border: none;
      color: var(--text-faint);
      padding: 2px;
    }

    .toast__close:hover {
      color: var(--text-strong);
    }

    @keyframes slide-in {
      from {
        opacity: 0;
        transform: translateY(8px);
      }
      to {
        opacity: 1;
        transform: none;
      }
    }
  `,
})
export class ToastContainer {
  protected readonly toasts = inject(ToastService);

  protected iconFor(tone: ToastTone): IconName {
    return TONE_ICON[tone];
  }
}
