import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { Icon } from './icon';

/**
 * Page selector.
 *
 * Long ranges are collapsed around the current page so the control keeps its width no
 * matter how big the catalogue grows.
 */
@Component({
  selector: 'app-pagination',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon],
  template: `
    @if (totalPages() > 1) {
      <nav class="pagination" aria-label="Paginación">
        <button
          type="button"
          class="pagination__step"
          [disabled]="page() === 0"
          aria-label="Página anterior"
          (click)="goTo.emit(page() - 1)"
        >
          <app-icon name="chevron-left" [size]="16" />
        </button>

        @for (slot of slots(); track $index) {
          @if (slot === null) {
            <span class="pagination__gap" aria-hidden="true">…</span>
          } @else {
            <button
              type="button"
              class="pagination__page"
              [class.is-current]="slot === page()"
              [attr.aria-current]="slot === page() ? 'page' : null"
              (click)="goTo.emit(slot)"
            >
              {{ slot + 1 }}
            </button>
          }
        }

        <button
          type="button"
          class="pagination__step"
          [disabled]="page() >= totalPages() - 1"
          aria-label="Página siguiente"
          (click)="goTo.emit(page() + 1)"
        >
          <app-icon name="chevron-right" [size]="16" />
        </button>
      </nav>
    }
  `,
  styles: `
    .pagination {
      display: flex;
      align-items: center;
      gap: var(--space-1);
    }

    .pagination__step,
    .pagination__page {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 34px;
      height: 34px;
      padding: 0 8px;
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      background: var(--surface-card);
      color: var(--text-muted);
      font-size: var(--text-sm);
      font-weight: 600;
      transition: all var(--duration-fast) var(--ease-out);
    }

    .pagination__step:not(:disabled):hover,
    .pagination__page:hover {
      background: var(--surface-sunken);
      color: var(--text-strong);
    }

    .pagination__step:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }

    .pagination__page.is-current {
      background: var(--brand-indigo);
      border-color: var(--brand-indigo);
      color: var(--text-on-dark);
    }

    .pagination__gap {
      padding: 0 4px;
      color: var(--text-faint);
    }
  `,
})
export class Pagination {
  readonly page = input.required<number>();
  readonly totalPages = input.required<number>();
  readonly goTo = output<number>();

  /** Page numbers to render; null marks a collapsed range. */
  protected readonly slots = computed<(number | null)[]>(() => {
    const total = this.totalPages();
    const current = this.page();

    if (total <= 7) {
      return Array.from({ length: total }, (_, index) => index);
    }

    const pages = new Set<number>([0, total - 1, current]);
    for (const offset of [-1, 1]) {
      const neighbour = current + offset;
      if (neighbour > 0 && neighbour < total - 1) {
        pages.add(neighbour);
      }
    }
    // Keep the control a stable width even at the ends of the range.
    if (current <= 2) {
      [1, 2, 3].forEach((page) => pages.add(page));
    }
    if (current >= total - 3) {
      [total - 2, total - 3, total - 4].forEach((page) => pages.add(page));
    }

    const ordered = [...pages].filter((page) => page >= 0 && page < total).sort((a, b) => a - b);
    const result: (number | null)[] = [];
    ordered.forEach((page, index) => {
      if (index > 0 && page - ordered[index - 1] > 1) {
        result.push(null);
      }
      result.push(page);
    });
    return result;
  });
}
