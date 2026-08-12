import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BookService } from '../../core/api/book.service';
import { ToastService } from '../../core/ui/toast.service';
import { Book, PageResponse } from '../../core/models/api.models';
import { BookCover } from '../../shared/ui/book-cover';
import { Icon } from '../../shared/ui/icon';
import { Pagination } from '../../shared/ui/pagination';
import { StatusBadge } from '../../shared/ui/status-badge';
import { EmptyState, ErrorState, Skeleton } from '../../shared/ui/feedback';

/**
 * Catalogue management as a table.
 *
 * The public catalogue is a grid built for browsing; this is built for maintenance, where
 * ISBN, status and the delete action matter more than covers.
 */
@Component({
  selector: 'app-admin-books',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, BookCover, Icon, Pagination, StatusBadge, EmptyState, ErrorState, Skeleton],
  templateUrl: './books.html',
  styleUrl: './books.scss',
})
export class AdminBooks {
  private readonly books = inject(BookService);
  private readonly toasts = inject(ToastService);

  protected readonly result = signal<PageResponse<Book> | null>(null);
  protected readonly loading = signal(true);
  protected readonly failed = signal(false);
  protected readonly search = signal('');
  protected readonly page = signal(0);
  protected readonly deletingId = signal<number | null>(null);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.failed.set(false);

    this.books.search({ search: this.search(), page: this.page(), size: 15, sort: 'title,asc' }).subscribe({
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

  protected submitSearch(): void {
    this.page.set(0);
    this.load();
  }

  protected goToPage(page: number): void {
    this.page.set(page);
    this.load();
  }

  protected remove(book: Book): void {
    this.deletingId.set(book.id);
    this.books.remove(book.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.toasts.success('Libro eliminado', `"${book.title}" ya no está en el catálogo.`);
        this.load();
      },
      error: () => this.deletingId.set(null),
    });
  }
}
