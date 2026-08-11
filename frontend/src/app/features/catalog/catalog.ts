import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BookService } from '../../core/api/book.service';
import { LoanService } from '../../core/api/loan.service';
import { ReservationService } from '../../core/api/reservation.service';
import { AuthStore } from '../../core/auth/auth.store';
import { ToastService } from '../../core/ui/toast.service';
import { Book, BookStatus, PageResponse } from '../../core/models/api.models';
import { Icon } from '../../shared/ui/icon';
import { Pagination } from '../../shared/ui/pagination';
import { EmptyState, ErrorState, Skeleton } from '../../shared/ui/feedback';
import { BookCard } from './book-card';
import { BookDetail } from './book-detail';

const PAGE_SIZE = 12;
const DEFAULT_SORT = 'createdAt,desc';

type FilterKey = 'q' | 'estado' | 'tema' | 'orden' | 'pagina';

@Component({
  selector: 'app-catalog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    RouterLink,
    Icon,
    Pagination,
    EmptyState,
    ErrorState,
    Skeleton,
    BookCard,
    BookDetail,
  ],
  templateUrl: './catalog.html',
  styleUrl: './catalog.scss',
})
export class Catalog {
  private readonly books = inject(BookService);
  private readonly loans = inject(LoanService);
  private readonly reservations = inject(ReservationService);
  private readonly toasts = inject(ToastService);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthStore);

  /**
   * Filters come from the URL, which makes a filtered catalogue linkable and keeps the
   * back button meaningful. They are read-only here: changing a filter is a navigation,
   * and the new URL flows back in through these inputs.
   */
  readonly q = input<string>();
  readonly estado = input<string>();
  readonly tema = input<string>();
  readonly orden = input<string>();
  readonly pagina = input<string>();

  protected readonly filters = computed(() => ({
    search: this.q() ?? '',
    status: (this.estado() ?? '') as BookStatus | '',
    subject: this.tema() ?? '',
    sort: this.orden() || DEFAULT_SORT,
    page: Number(this.pagina() ?? 0) || 0,
  }));

  /** What is typed in the search box before it is submitted. */
  protected readonly searchDraft = signal('');

  protected readonly result = signal<PageResponse<Book> | null>(null);
  protected readonly subjects = signal<string[]>([]);
  protected readonly loading = signal(true);
  protected readonly failed = signal(false);
  protected readonly selected = signal<Book | null>(null);
  protected readonly busyBookId = signal<number | null>(null);

  protected readonly hasFilters = computed(() => {
    const { search, status, subject } = this.filters();
    return !!(search || status || subject);
  });

  protected readonly sortOptions = [
    { value: DEFAULT_SORT, label: 'Más reciente' },
    { value: 'title,asc', label: 'Título (A-Z)' },
    { value: 'author,asc', label: 'Autor (A-Z)' },
    { value: 'publicationYear,desc', label: 'Año de publicación' },
  ];

  constructor() {
    effect(() => {
      const filters = this.filters();
      // Safe to write here: nothing in this effect reads the draft back.
      this.searchDraft.set(filters.search);
      this.load();
    });

    this.books.subjects().subscribe({
      next: (subjects) => this.subjects.set(subjects),
      // A missing filter list degrades the screen, it does not break it.
      error: () => this.subjects.set([]),
    });
  }

  protected load(): void {
    this.loading.set(true);
    this.failed.set(false);

    this.books.search({ ...this.filters(), size: PAGE_SIZE }).subscribe({
      next: (result) => {
        this.result.set(result);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.failed.set(true);
      },
    });
  }

  private navigate(overrides: Partial<Record<FilterKey, string | null>>): void {
    const current = this.filters();
    const params: Record<string, string | null> = {
      q: current.search || null,
      estado: current.status || null,
      tema: current.subject || null,
      orden: current.sort === DEFAULT_SORT ? null : current.sort,
      pagina: current.page === 0 ? null : String(current.page),
      ...overrides,
    };
    void this.router.navigate(['/catalogo'], { queryParams: params });
  }

  protected submitSearch(): void {
    this.navigate({ q: this.searchDraft().trim() || null, pagina: null });
  }

  protected changeStatus(value: string): void {
    this.navigate({ estado: value || null, pagina: null });
  }

  protected changeSubject(value: string): void {
    this.navigate({ tema: value || null, pagina: null });
  }

  protected changeSort(value: string): void {
    this.navigate({ orden: value === DEFAULT_SORT ? null : value, pagina: null });
  }

  protected goToPage(page: number): void {
    this.navigate({ pagina: page === 0 ? null : String(page) });
  }

  protected clearFilters(): void {
    void this.router.navigate(['/catalogo']);
  }

  protected borrow(book: Book): void {
    this.busyBookId.set(book.id);
    this.loans.create(book.id).subscribe({
      next: (loan) => {
        this.busyBookId.set(null);
        this.selected.set(null);
        this.toasts.success(
          'Préstamo registrado',
          `Devuelve "${book.title}" antes del ${new Date(loan.dueDate).toLocaleDateString('es-CL')}.`,
        );
        this.load();
      },
      // The interceptor already surfaced whichever rule rejected this.
      error: () => this.busyBookId.set(null),
    });
  }

  protected reserve(book: Book): void {
    this.busyBookId.set(book.id);
    this.reservations.create(book.id).subscribe({
      next: (reservation) => {
        this.busyBookId.set(null);
        this.selected.set(null);
        this.toasts.success(
          'Reserva creada',
          `Eres el número ${reservation.queuePosition} de la fila. Te avisaremos por correo.`,
        );
        this.load();
      },
      error: () => this.busyBookId.set(null),
    });
  }

  protected remove(book: Book): void {
    this.busyBookId.set(book.id);
    this.books.remove(book.id).subscribe({
      next: () => {
        this.busyBookId.set(null);
        this.selected.set(null);
        this.toasts.success('Libro eliminado', `"${book.title}" ya no está en el catálogo.`);
        this.load();
      },
      error: () => this.busyBookId.set(null),
    });
  }
}
