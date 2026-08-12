import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { Book } from '../../core/models/api.models';
import { BookCover } from '../../shared/ui/book-cover';
import { Icon } from '../../shared/ui/icon';
import { Modal } from '../../shared/ui/modal';
import { StatusBadge } from '../../shared/ui/status-badge';

/**
 * Book detail and the actions available on it.
 *
 * Which actions appear is driven by the same rules the server enforces, so a reader is
 * not offered a button that can only end in a 409: a copy on the shelf can be borrowed,
 * one that is out can be queued for, and only an ADMIN sees the delete action — and only
 * while the copy is actually available.
 */
@Component({
  selector: 'app-book-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Modal, BookCover, StatusBadge, Icon],
  template: `
    <app-modal [heading]="book().title" [width]="620" (dismiss)="close.emit()">
      <div class="detail">
        <app-book-cover class="detail__cover" [url]="book().coverUrl" [title]="book().title" />

        <div class="detail__info">
          <div class="detail__headline">
            <span class="detail__author">{{ book().author }}</span>
            <app-status-badge [status]="book().status" />
          </div>

          <dl class="detail__facts">
            <div>
              <dt>ISBN</dt>
              <dd>{{ book().isbn }}</dd>
            </div>
            @if (book().publicationYear) {
              <div>
                <dt>Publicado</dt>
                <dd>{{ book().publicationYear }}</dd>
              </div>
            }
          </dl>

          @if (book().description) {
            <p class="detail__description">{{ book().description }}</p>
          }

          @if (book().subjects.length) {
            <div class="detail__subjects">
              @for (subject of book().subjects; track subject) {
                <span class="chip">{{ subject }}</span>
              }
            </div>
          }

          @if (blocked()) {
            <div class="note note--warning">
              <app-icon name="lock" [size]="16" class="note__icon" />
              <span>Tu cuenta está bloqueada temporalmente, por lo que no puedes pedir préstamos.</span>
            </div>
          } @else if (book().status === 'RESERVADO') {
            <div class="note">
              <app-icon name="info" [size]="16" class="note__icon" />
              <span>Este ejemplar está reservado para el primer lector de la lista de espera.</span>
            </div>
          }
        </div>
      </div>

      <footer class="detail__actions" modalFooter>
        @if (canDelete()) {
          <button type="button" class="btn btn--danger" [disabled]="busy()" (click)="remove.emit(book())">
            <app-icon name="trash" [size]="16" />
            Eliminar
          </button>
        }
        <span class="detail__spacer"></span>
        <button type="button" class="btn btn--secondary" (click)="close.emit()">Cerrar</button>

        @if (book().status === 'DISPONIBLE') {
          <button type="button" class="btn btn--primary" [disabled]="busy() || blocked()" (click)="borrow.emit(book())">
            <app-icon name="book" [size]="16" />
            Pedir prestado
          </button>
        } @else if (book().status === 'PRESTADO') {
          <button type="button" class="btn btn--primary" [disabled]="busy()" (click)="reserve.emit(book())">
            <app-icon name="bookmark" [size]="16" />
            Reservar
          </button>
        }
      </footer>
    </app-modal>
  `,
  styleUrl: './book-detail.scss',
})
export class BookDetail {
  readonly book = input.required<Book>();
  readonly isAdmin = input(false);
  readonly blocked = input(false);
  readonly busy = input(false);

  readonly close = output<void>();
  readonly borrow = output<Book>();
  readonly reserve = output<Book>();
  readonly remove = output<Book>();

  /** The server only allows removing a copy that is on the shelf. */
  protected readonly canDelete = computed(() => this.isAdmin() && this.book().status === 'DISPONIBLE');
}
