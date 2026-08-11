import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { BookService } from '../../core/api/book.service';
import { LoanService } from '../../core/api/loan.service';
import { AuthStore } from '../../core/auth/auth.store';
import { BookRecommendation, Loan } from '../../core/models/api.models';
import { BookCover } from '../../shared/ui/book-cover';
import { Icon } from '../../shared/ui/icon';
import { StatCard } from '../../shared/ui/stat-card';
import { StatusBadge } from '../../shared/ui/status-badge';
import { EmptyState, ErrorState, Skeleton } from '../../shared/ui/feedback';

/**
 * The reader's own home screen.
 *
 * The mock-up shows library-wide totals here, but the endpoint behind them is
 * ADMIN-only: a librarian landing on this page would get a wall of 403s. So this screen
 * shows what the reader actually owns — their loans, what is about to fall due, and what
 * they might read next — and the global numbers live in Administración → Resumen.
 */
@Component({
  selector: 'app-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    RouterLink,
    BookCover,
    Icon,
    StatCard,
    StatusBadge,
    EmptyState,
    ErrorState,
    Skeleton,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly loans = inject(LoanService);
  private readonly books = inject(BookService);
  protected readonly auth = inject(AuthStore);

  protected readonly myLoans = signal<Loan[]>([]);
  protected readonly availableBooks = signal(0);
  protected readonly recommendations = signal<BookRecommendation[]>([]);
  protected readonly loading = signal(true);
  protected readonly failed = signal(false);

  protected readonly firstName = computed(() => this.auth.displayName().split(/\s+/)[0] ?? '');

  protected readonly activeLoans = computed(() => this.myLoans().filter((loan) => !loan.returnDate));
  protected readonly dueSoon = computed(() => this.myLoans().filter((loan) => loan.status === 'POR_VENCER'));
  protected readonly overdue = computed(() => this.myLoans().filter((loan) => loan.status === 'VENCIDO'));

  /** Nearest due date first: that is the one worth acting on. */
  protected readonly upcoming = computed(() =>
    [...this.activeLoans()].sort((a, b) => a.dueDate.localeCompare(b.dueDate)).slice(0, 4),
  );

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.failed.set(false);

    forkJoin({
      loans: this.loans.mine(0, 50),
      catalogue: this.books.search({ status: 'DISPONIBLE', size: 1 }),
    }).subscribe({
      next: ({ loans, catalogue }) => {
        this.myLoans.set(loans.content);
        this.availableBooks.set(catalogue.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.failed.set(true);
      },
    });

    // Recommendations are a bonus, so a failure here must not take the screen down.
    this.books.recommendations(3).subscribe({
      next: (recommendations) => this.recommendations.set(recommendations),
      error: () => this.recommendations.set([]),
    });
  }

  protected dueHint(loan: Loan): string {
    if (loan.daysLate > 0) {
      return `Vencido hace ${loan.daysLate} ${loan.daysLate === 1 ? 'día' : 'días'}`;
    }
    if (loan.daysUntilDue === 0) {
      return 'Vence hoy';
    }
    return `${loan.daysUntilDue} ${loan.daysUntilDue === 1 ? 'día' : 'días'}`;
  }
}
