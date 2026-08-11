import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LoanService } from '../../core/api/loan.service';
import { ReservationService } from '../../core/api/reservation.service';
import { ToastService } from '../../core/ui/toast.service';
import { Reservation } from '../../core/models/api.models';
import { BookCover } from '../../shared/ui/book-cover';
import { Icon } from '../../shared/ui/icon';
import { StatusBadge } from '../../shared/ui/status-badge';
import { EmptyState, ErrorState, Skeleton } from '../../shared/ui/feedback';

type Tab = 'activas' | 'historial';

@Component({
  selector: 'app-my-reservations',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, RouterLink, BookCover, Icon, StatusBadge, EmptyState, ErrorState, Skeleton],
  templateUrl: './my-reservations.html',
  styleUrl: './my-reservations.scss',
})
export class MyReservations {
  private readonly reservations = inject(ReservationService);
  private readonly loans = inject(LoanService);
  private readonly toasts = inject(ToastService);

  protected readonly all = signal<Reservation[]>([]);
  protected readonly loading = signal(true);
  protected readonly failed = signal(false);
  protected readonly tab = signal<Tab>('activas');
  protected readonly busyId = signal<number | null>(null);

  protected readonly active = computed(() =>
    this.all().filter((reservation) => reservation.status === 'PENDIENTE' || reservation.status === 'NOTIFICADO'),
  );
  protected readonly history = computed(() =>
    this.all().filter((reservation) => reservation.status === 'CANCELADO' || reservation.status === 'CUMPLIDO'),
  );

  protected readonly visible = computed(() => (this.tab() === 'activas' ? this.active() : this.history()));

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.failed.set(false);

    this.reservations.mine().subscribe({
      next: (reservations) => {
        this.all.set(reservations);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.failed.set(true);
      },
    });
  }

  protected cancel(reservation: Reservation): void {
    this.busyId.set(reservation.id);
    this.reservations.cancel(reservation.id).subscribe({
      next: () => {
        this.busyId.set(null);
        this.toasts.success('Reserva cancelada', `Saliste de la lista de espera de "${reservation.book.title}".`);
        this.load();
      },
      error: () => this.busyId.set(null),
    });
  }

  /**
   * Turning a hold into a loan. The reservation is closed by the server as a side effect
   * of the loan, so there is no separate "mark as received" call to make.
   */
  protected borrow(reservation: Reservation): void {
    this.busyId.set(reservation.id);
    this.loans.create(reservation.book.id).subscribe({
      next: (loan) => {
        this.busyId.set(null);
        this.toasts.success(
          'Préstamo registrado',
          `Devuelve "${loan.book.title}" antes del ${new Date(loan.dueDate).toLocaleDateString('es-CL')}.`,
        );
        this.load();
      },
      error: () => this.busyId.set(null),
    });
  }

  protected positionLabel(reservation: Reservation): string {
    if (reservation.status === 'NOTIFICADO') {
      return 'Reservado para ti';
    }
    return reservation.queuePosition ? `#${reservation.queuePosition} en la fila` : '—';
  }
}
