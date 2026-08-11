import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { Icon } from './icon';

/**
 * Dialog shell.
 *
 * Escape and a click on the backdrop both close it, which is what people expect; the
 * click handler checks the target so a drag that ends outside the panel does not.
 */
@Component({
  selector: 'app-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon],
  template: `
    <div
      class="backdrop"
      role="presentation"
      (click)="onBackdropClick($event)"
      (keydown.escape)="dismiss.emit()"
      tabindex="-1"
    >
      <div
        class="panel"
        [style.max-width.px]="width()"
        role="dialog"
        aria-modal="true"
        [attr.aria-label]="heading()"
      >
        <header class="panel__header">
          <h2 class="panel__title">{{ heading() }}</h2>
          <button type="button" class="panel__close" aria-label="Cerrar" (click)="dismiss.emit()">
            <app-icon name="close" [size]="18" />
          </button>
        </header>
        <div class="panel__body">
          <ng-content />
        </div>
        <ng-content select="[modalFooter]" />
      </div>
    </div>
  `,
  styles: `
    .backdrop {
      position: fixed;
      inset: 0;
      z-index: 90;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: var(--space-6);
      background: rgba(24, 34, 72, 0.45);
      backdrop-filter: blur(2px);
      animation: fade-in var(--duration-fast) var(--ease-out);
    }

    .panel {
      width: 100%;
      max-height: calc(100vh - 64px);
      display: flex;
      flex-direction: column;
      background: var(--surface-card);
      border-radius: var(--radius-xl);
      box-shadow: var(--shadow-lg);
      overflow: hidden;
      animation: rise var(--duration-base) var(--ease-out);
    }

    .panel__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: var(--space-4);
      padding: var(--space-5) var(--space-6);
      border-bottom: 1px solid var(--border-subtle);
    }

    .panel__title {
      font-size: var(--text-lg);
    }

    .panel__close {
      display: flex;
      padding: 6px;
      border: none;
      border-radius: var(--radius-sm);
      background: none;
      color: var(--text-muted);
    }

    .panel__close:hover {
      background: var(--surface-sunken);
      color: var(--text-strong);
    }

    .panel__body {
      padding: var(--space-6);
      overflow-y: auto;
    }

    @keyframes fade-in {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    @keyframes rise {
      from { opacity: 0; transform: translateY(12px) scale(0.99); }
      to { opacity: 1; transform: none; }
    }
  `,
})
export class Modal {
  readonly heading = input.required<string>();
  readonly width = input(560);
  readonly dismiss = output<void>();

  protected onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.dismiss.emit();
    }
  }
}
