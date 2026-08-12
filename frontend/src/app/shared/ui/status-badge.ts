import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { BookStatus, LoanStatus, ReservationStatus, UserAccountStatus } from '../../core/models/api.models';

type AnyStatus = BookStatus | LoanStatus | ReservationStatus | UserAccountStatus;
type Tone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

/**
 * Single source of truth for how a status looks and reads.
 *
 * The API speaks in enum names; the interface speaks Spanish and colour. Mapping that
 * once here is what stops the catalogue, the loans table and the dashboard from drifting
 * into three different greens for the same idea.
 */
const PRESENTATION: Record<AnyStatus, { label: string; tone: Tone }> = {
  DISPONIBLE: { label: 'Disponible', tone: 'success' },
  PRESTADO: { label: 'Prestado', tone: 'warning' },
  RESERVADO: { label: 'Reservado', tone: 'info' },

  ACTIVO: { label: 'Activo', tone: 'success' },
  POR_VENCER: { label: 'Por vencer', tone: 'warning' },
  VENCIDO: { label: 'Vencido', tone: 'danger' },
  DEVUELTO: { label: 'Devuelto', tone: 'neutral' },

  PENDIENTE: { label: 'Pendiente', tone: 'warning' },
  NOTIFICADO: { label: 'Notificado', tone: 'info' },
  CANCELADO: { label: 'Cancelada', tone: 'neutral' },
  CUMPLIDO: { label: 'Cumplida', tone: 'success' },

  ADVERTENCIA: { label: 'Advertencia', tone: 'warning' },
  BLOQUEADO: { label: 'Bloqueado', tone: 'danger' },
};

@Component({
  selector: 'app-status-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="badge" [class]="'badge--' + tone()">{{ label() }}</span>`,
  styles: `
    .badge {
      display: inline-flex;
      align-items: center;
      padding: 3px 10px;
      border-radius: var(--radius-pill);
      border: 1px solid transparent;
      font-size: var(--text-xs);
      font-weight: 600;
      white-space: nowrap;
    }

    .badge--success {
      background: var(--state-success-bg);
      border-color: var(--state-success-border);
      color: var(--state-success-fg);
    }

    .badge--warning {
      background: var(--state-warning-bg);
      border-color: var(--state-warning-border);
      color: var(--state-warning-fg);
    }

    .badge--danger {
      background: var(--state-danger-bg);
      border-color: var(--state-danger-border);
      color: var(--state-danger-fg);
    }

    .badge--info {
      background: var(--state-info-bg);
      border-color: var(--state-info-border);
      color: var(--state-info-fg);
    }

    .badge--neutral {
      background: var(--state-neutral-bg);
      border-color: var(--state-neutral-border);
      color: var(--state-neutral-fg);
    }
  `,
})
export class StatusBadge {
  readonly status = input.required<AnyStatus>();

  protected readonly label = computed(() => PRESENTATION[this.status()]?.label ?? this.status());
  protected readonly tone = computed(() => PRESENTATION[this.status()]?.tone ?? 'neutral');
}
