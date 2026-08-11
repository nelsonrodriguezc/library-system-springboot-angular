import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Icon, IconName } from './icon';

type Tone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

/** One headline number, as used across the dashboards. */
@Component({
  selector: 'app-stat-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon],
  template: `
    <div class="stat">
      <div class="stat__top">
        <span class="stat__label">{{ label() }}</span>
        <span class="stat__icon" [class]="'stat__icon--' + tone()">
          <app-icon [name]="icon()" [size]="18" />
        </span>
      </div>
      <strong class="stat__value">{{ formatted() }}</strong>
      @if (hint()) {
        <span class="stat__hint" [class]="'stat__hint--' + tone()">{{ hint() }}</span>
      }
    </div>
  `,
  styles: `
    :host {
      display: block;
    }

    .stat {
      display: flex;
      flex-direction: column;
      gap: 6px;
      height: 100%;
      padding: var(--space-5);
      background: var(--surface-card);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-xs);
    }

    .stat__top {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: var(--space-3);
    }

    .stat__label {
      font-size: var(--text-sm);
      color: var(--text-muted);
      font-weight: 500;
    }

    .stat__icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 34px;
      height: 34px;
      border-radius: var(--radius-md);
      flex-shrink: 0;
    }

    .stat__icon--success { background: var(--state-success-bg); color: var(--state-success-fg); }
    .stat__icon--warning { background: var(--state-warning-bg); color: var(--state-warning-fg); }
    .stat__icon--danger { background: var(--state-danger-bg); color: var(--state-danger-fg); }
    .stat__icon--info { background: var(--state-info-bg); color: var(--state-info-fg); }
    .stat__icon--neutral { background: var(--state-neutral-bg); color: var(--state-neutral-fg); }

    .stat__value {
      font-size: var(--text-3xl);
      font-weight: 700;
      letter-spacing: -0.02em;
      line-height: 1.15;
    }

    .stat__hint {
      font-size: var(--text-xs);
      color: var(--text-muted);
    }

    .stat__hint--danger { color: var(--state-danger-fg); }
    .stat__hint--warning { color: var(--state-warning-fg); }
    .stat__hint--success { color: var(--state-success-fg); }
  `,
})
export class StatCard {
  readonly label = input.required<string>();
  readonly value = input.required<number>();
  readonly icon = input<IconName>('book');
  readonly tone = input<Tone>('info');
  readonly hint = input<string>();

  /** Thousands separators, Chilean style: 1.258 rather than 1,258. */
  protected readonly formatted = computed(() => new Intl.NumberFormat('es-CL').format(this.value()));
}
