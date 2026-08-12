import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { BookService } from '../../core/api/book.service';
import { apiErrorMessage } from '../../core/http/error.interceptor';
import { ToastService } from '../../core/ui/toast.service';
import { BookPreview } from '../../core/models/api.models';
import { BookCover } from '../../shared/ui/book-cover';
import { Icon } from '../../shared/ui/icon';
import { isbnValidator, normaliseIsbn } from './isbn.validator';

type Step = 1 | 2 | 3;

/**
 * Three-step registration: look the ISBN up, review what came back, confirm.
 *
 * The whole point of the flow is that step one is optional in effect — when Open Library
 * is unreachable, and it often is, the wizard says so and lets the librarian type the
 * record by hand rather than dead-ending. The server behaves the same way: what is typed
 * always wins over what the external catalogue guessed.
 */
@Component({
  selector: 'app-add-book',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, Icon, BookCover],
  templateUrl: './add-book.html',
  styleUrl: './add-book.scss',
})
export class AddBook {
  private readonly formBuilder = inject(FormBuilder);
  private readonly books = inject(BookService);
  private readonly toasts = inject(ToastService);
  private readonly router = inject(Router);

  protected readonly step = signal<Step>(1);
  protected readonly looking = signal(false);
  protected readonly saving = signal(false);
  protected readonly lookupFailed = signal<string | null>(null);
  protected readonly preview = signal<BookPreview | null>(null);

  protected readonly isbnForm = this.formBuilder.nonNullable.group({
    isbn: ['', [Validators.required, isbnValidator]],
  });

  protected readonly detailsForm = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(250)]],
    author: ['', [Validators.required, Validators.maxLength(180)]],
    publicationYear: [null as number | null, [Validators.min(1000), Validators.max(2200)]],
    description: [''],
    coverUrl: [''],
  });

  protected readonly subjects = signal<string[]>([]);

  protected readonly steps = [
    { number: 1 as Step, label: 'Buscar' },
    { number: 2 as Step, label: 'Información' },
    { number: 3 as Step, label: 'Confirmar' },
  ];

  protected readonly normalisedIsbn = computed(() => normaliseIsbn(this.isbnForm.controls.isbn.value));

  protected lookup(): void {
    if (this.isbnForm.invalid || this.looking()) {
      this.isbnForm.markAllAsTouched();
      return;
    }

    this.looking.set(true);
    this.lookupFailed.set(null);

    this.books.lookup(this.normalisedIsbn()).subscribe({
      next: (preview) => {
        this.looking.set(false);
        this.preview.set(preview);
        this.applyPreview(preview);
        this.step.set(2);
      },
      error: (response: unknown) => {
        this.looking.set(false);
        this.preview.set(null);
        this.lookupFailed.set(
          apiErrorMessage(
            response,
            'No pudimos obtener información desde el catálogo externo. Puedes completar los datos a mano.',
          ),
        );
      },
    });
  }

  /** Skips straight to the manual form, used when the lookup found nothing. */
  protected fillByHand(): void {
    this.preview.set(null);
    this.subjects.set([]);
    this.detailsForm.reset({ title: '', author: '', publicationYear: null, description: '', coverUrl: '' });
    this.step.set(2);
  }

  protected back(): void {
    this.step.update((current) => (current === 1 ? 1 : ((current - 1) as Step)));
  }

  protected toConfirmation(): void {
    if (this.detailsForm.invalid) {
      this.detailsForm.markAllAsTouched();
      return;
    }
    this.step.set(3);
  }

  protected save(): void {
    if (this.detailsForm.invalid || this.saving()) {
      return;
    }
    const details = this.detailsForm.getRawValue();
    this.saving.set(true);

    this.books
      .create({
        isbn: this.normalisedIsbn(),
        title: details.title.trim(),
        author: details.author.trim(),
        publicationYear: details.publicationYear,
        description: details.description?.trim() || null,
        coverUrl: details.coverUrl?.trim() || null,
        subjects: this.subjects(),
      })
      .subscribe({
        next: (book) => {
          this.saving.set(false);
          this.toasts.success('Libro agregado', `"${book.title}" ya está disponible en el catálogo.`);
          void this.router.navigate(['/catalogo'], { queryParams: { q: book.isbn } });
        },
        // The interceptor already reported the reason, for example a duplicate ISBN.
        error: () => this.saving.set(false),
      });
  }

  protected removeSubject(subject: string): void {
    this.subjects.update((current) => current.filter((item) => item !== subject));
  }

  protected invalidDetail(control: 'title' | 'author' | 'publicationYear'): boolean {
    const field = this.detailsForm.controls[control];
    return field.invalid && field.touched;
  }

  private applyPreview(preview: BookPreview): void {
    this.detailsForm.patchValue({
      title: preview.title ?? '',
      author: preview.author ?? '',
      publicationYear: preview.publicationYear,
      description: preview.description ?? '',
      coverUrl: preview.coverUrl ?? '',
    });
    this.subjects.set(preview.subjects ?? []);
  }
}
