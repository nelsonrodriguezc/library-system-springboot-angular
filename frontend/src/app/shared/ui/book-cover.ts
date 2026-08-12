import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';

/**
 * A book cover with a graceful fallback.
 *
 * Covers come from Open Library and a good share of ISBNs have none, so the image
 * failing is the normal case, not the exception: when it does, the title's initials are
 * drawn instead of leaving a broken image in the grid.
 */
@Component({
  selector: 'app-book-cover',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (url() && !failed()) {
      <img [src]="url()" [alt]="'Portada de ' + title()" loading="lazy" (error)="failed.set(true)" />
    } @else {
      <span class="placeholder" aria-hidden="true">{{ initials() }}</span>
    }
  `,
  styles: `
    :host {
      display: block;
      position: relative;
      overflow: hidden;
      border-radius: var(--radius-sm);
      background: var(--surface-sunken);
      border: 1px solid var(--border-subtle);
    }

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .placeholder {
      position: absolute;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, var(--brand-navy) 0%, var(--brand-navy-soft) 100%);
      color: var(--text-on-dark);
      font-weight: 700;
      font-size: clamp(12px, 28cqw, 26px);
      letter-spacing: 0.02em;
      container-type: inline-size;
    }
  `,
})
export class BookCover {
  readonly url = input<string | null>(null);
  readonly title = input('');

  protected readonly failed = signal(false);

  protected initials(): string {
    return this.title()
      .split(/\s+/)
      .filter((word) => word.length > 2)
      .slice(0, 2)
      .map((word) => word[0]?.toUpperCase() ?? '')
      .join('');
  }
}
