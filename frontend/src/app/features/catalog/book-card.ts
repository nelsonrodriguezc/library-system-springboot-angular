import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { Book } from '../../core/models/api.models';
import { BookCover } from '../../shared/ui/book-cover';
import { StatusBadge } from '../../shared/ui/status-badge';

@Component({
  selector: 'app-book-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BookCover, StatusBadge],
  template: `
    <button type="button" class="card-book" (click)="open.emit(book())">
      <app-book-cover class="card-book__cover" [url]="book().coverUrl" [title]="book().title" />
      <div class="card-book__meta">
        <strong class="card-book__title">{{ book().title }}</strong>
        <span class="card-book__author">{{ book().author }}</span>
        @if (book().publicationYear) {
          <span class="card-book__year">{{ book().publicationYear }}</span>
        }
      </div>
      <app-status-badge [status]="book().status" />
    </button>
  `,
  styles: `
    :host {
      display: block;
    }

    .card-book {
      display: flex;
      flex-direction: column;
      gap: var(--space-3);
      width: 100%;
      height: 100%;
      padding: var(--space-4);
      background: var(--surface-card);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-xs);
      text-align: left;
      transition: transform var(--duration-fast) var(--ease-out),
        box-shadow var(--duration-fast) var(--ease-out),
        border-color var(--duration-fast) var(--ease-out);
    }

    .card-book:hover {
      transform: translateY(-2px);
      border-color: var(--brand-indigo-border);
      box-shadow: var(--shadow-md);
    }

    .card-book__cover {
      width: 100%;
      aspect-ratio: 3 / 4;
    }

    .card-book__meta {
      display: flex;
      flex-direction: column;
      gap: 2px;
      flex: 1;
    }

    .card-book__title {
      font-size: var(--text-base);
      font-weight: 650;
      color: var(--text-strong);
      line-height: 1.35;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .card-book__author {
      font-size: var(--text-sm);
      color: var(--text-muted);
    }

    .card-book__year {
      font-size: var(--text-xs);
      color: var(--text-faint);
    }

    @media (prefers-reduced-motion: reduce) {
      .card-book:hover {
        transform: none;
      }
    }
  `,
})
export class BookCard {
  readonly book = input.required<Book>();
  readonly open = output<Book>();
}
