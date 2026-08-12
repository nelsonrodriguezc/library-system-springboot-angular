import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../core/api/admin.service';
import { ToastService } from '../../core/ui/toast.service';
import { AdminStats } from '../../core/models/api.models';
import { DonutChart, DonutSegment } from '../../shared/ui/donut-chart';
import { LineChart, LinePoint } from '../../shared/ui/line-chart';
import { Icon } from '../../shared/ui/icon';
import { StatCard } from '../../shared/ui/stat-card';
import { StatusBadge } from '../../shared/ui/status-badge';
import { EmptyState, ErrorState, Skeleton } from '../../shared/ui/feedback';

@Component({
  selector: 'app-admin-overview',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    DonutChart,
    LineChart,
    Icon,
    StatCard,
    StatusBadge,
    EmptyState,
    ErrorState,
    Skeleton,
  ],
  templateUrl: './overview.html',
  styleUrl: './overview.scss',
})
export class AdminOverview {
  private readonly admin = inject(AdminService);
  private readonly toasts = inject(ToastService);

  protected readonly stats = signal<AdminStats | null>(null);
  protected readonly loading = signal(true);
  protected readonly failed = signal(false);
  protected readonly sendingJob = signal<'reminders' | 'overdue' | null>(null);

  /**
   * The four buckets are mutually exclusive on the server, which is what lets this add up
   * to 100% instead of counting a loan as both open and about to fall due.
   */
  protected readonly loanSegments = computed<DonutSegment[]>(() => {
    const byStatus = this.stats()?.loansByStatus;
    if (!byStatus) {
      return [];
    }
    return [
      { label: 'Activos', value: byStatus.active, color: 'var(--chart-active)' },
      { label: 'Por vencer', value: byStatus.dueSoon, color: 'var(--chart-due-soon)' },
      { label: 'Vencidos', value: byStatus.overdue, color: 'var(--chart-overdue)' },
      { label: 'Devueltos', value: byStatus.returned, color: 'var(--chart-returned)' },
    ];
  });

  protected readonly monthlyPoints = computed<LinePoint[]>(
    () => this.stats()?.loansPerMonth.map((month) => ({ label: month.label, total: month.total })) ?? [],
  );

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.failed.set(false);

    this.admin.stats().subscribe({
      next: (stats) => {
        this.stats.set(stats);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.failed.set(true);
      },
    });
  }

  /**
   * Runs a scheduled sweep on demand. The jobs are idempotent, so pressing this twice
   * cannot send the same message twice — that is what the sent counter reflects.
   */
  protected runReminders(): void {
    this.sendingJob.set('reminders');
    this.admin.sendDueSoonReminders().subscribe({
      next: ({ sent }) => {
        this.sendingJob.set(null);
        this.reportJob(sent, 'recordatorio');
      },
      error: () => this.sendingJob.set(null),
    });
  }

  protected runOverdueNotices(): void {
    this.sendingJob.set('overdue');
    this.admin.sendOverdueNotices().subscribe({
      next: ({ sent }) => {
        this.sendingJob.set(null);
        this.reportJob(sent, 'aviso de vencimiento');
      },
      error: () => this.sendingJob.set(null),
    });
  }

  private reportJob(sent: number, noun: string): void {
    if (sent === 0) {
      this.toasts.info('No había nada que enviar', `Ningún préstamo necesitaba un ${noun} en este momento.`);
      return;
    }
    this.toasts.success(
      `Se ${sent === 1 ? 'envió' : 'enviaron'} ${sent} ${sent === 1 ? noun : noun + 's'}`,
      'Puedes revisarlos en MailHog (localhost:8025).',
    );
  }
}
