import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LoanService } from '../../core/api/loan.service';
import { AuthStore } from '../../core/auth/auth.store';
import { ToastService } from '../../core/ui/toast.service';
import { Loan } from '../../core/models/api.models';
import { BookCover } from '../../shared/ui/book-cover';
import { Icon } from '../../shared/ui/icon';
import { StatusBadge } from '../../shared/ui/status-badge';
import { EmptyState, ErrorState, Skeleton } from '../../shared/ui/feedback';

type Tab = 'activos' | 'vencidos' | 'historial';

@Component({
  selector: 'app-my-loans',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, RouterLink, BookCover, Icon, StatusBadge, EmptyState, ErrorState, Skeleton],
  templateUrl: './my-loans.html',
  styleUrl: './my-loans.scss',
})
export class MyLoans {
  private readonly loans = inject(LoanService);
  private readonly toasts = inject(ToastService);
  protected readonly auth = inject(AuthStore);

  protected readonly all = signal<Loan[]>([]);
  protected readonly loading = signal(true);
  protected readonly failed = signal(false);
  protected readonly tab = signal<Tab>('activos');
  protected readonly returningId = signal<number | null>(null);

  protected readonly active = computed(() => this.all().filter((loan) => !loan.returnDate));
  protected readonly overdue = computed(() => this.all().filter((loan) => loan.status === 'VENCIDO'));
  protected readonly history = computed(() => this.all().filter((loan) => loan.returnDate));

  protected readonly visible = computed(() => {
    switch (this.tab()) {
      case 'activos':
        return this.active();
      case 'vencidos':
        return this.overdue();
      default:
        return this.history();
    }
  });

  protected readonly maxActive = 3;
  protected readonly remaining = computed(() => Math.max(this.maxActive - this.active().length, 0));

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.failed.set(false);

    this.loans.mine().subscribe({
      next: (page) => {
        this.all.set(page.content);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.failed.set(true);
      },
    });
  }

  protected returnLoan(loan: Loan): void {
    this.returningId.set(loan.id);
    this.loans.return(loan.id).subscribe({
      next: () => {
        this.returningId.set(null);
        this.toasts.success('Libro devuelto', `Gracias por devolver "${loan.book.title}".`);
        this.load();
      },
      error: () => this.returningId.set(null),
    });
  }

  /** Wording for the due-date column, which is where the countdown belongs. */
  protected dueHint(loan: Loan): string | null {
    if (loan.returnDate) {
      return null;
    }
    if (loan.daysLate > 0) {
      return `Vencido hace ${loan.daysLate} ${loan.daysLate === 1 ? 'día' : 'días'}`;
    }
    if (loan.daysUntilDue === 0) {
      return 'Vence hoy';
    }
    return `${loan.daysUntilDue} ${loan.daysUntilDue === 1 ? 'día restante' : 'días restantes'}`;
  }

  protected isLate(loan: Loan): boolean {
    return !loan.returnDate && loan.daysLate > 0;
  }
}
