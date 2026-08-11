import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthStore } from '../../core/auth/auth.store';
import { apiErrorMessage } from '../../core/http/error.interceptor';
import { ToastService } from '../../core/ui/toast.service';
import { Icon } from '../../shared/ui/icon';
import { AuthPanel } from './auth-panel';

/**
 * Public sign-up. The server always creates a BIBLIOTECARIO account here, which is why
 * no role is asked for: handing out administrator rights through an unauthenticated
 * endpoint would make the role checks on the rest of the API pointless.
 */
@Component({
  selector: 'app-register',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, Icon, AuthPanel],
  templateUrl: './register.html',
  styleUrl: './auth-form.scss',
})
export class Register {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastService);

  protected readonly submitting = signal(false);
  protected readonly failure = signal<string | null>(null);
  protected readonly passwordVisible = signal(false);

  /** Mirrors the server's constraints so the reader is told before the round trip. */
  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(180)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const { name, email, password } = this.form.getRawValue();
    this.submitting.set(true);
    this.failure.set(null);

    this.auth.register(name.trim(), email.trim(), password).subscribe({
      next: () => {
        this.submitting.set(false);
        this.toasts.success('Cuenta creada', 'Ya puedes explorar el catálogo y pedir préstamos.');
        void this.router.navigateByUrl('/inicio');
      },
      error: (response: unknown) => {
        this.submitting.set(false);
        this.failure.set(apiErrorMessage(response, 'No pudimos crear la cuenta. Inténtalo nuevamente.'));
      },
    });
  }

  protected invalid(control: 'name' | 'email' | 'password'): boolean {
    const field = this.form.controls[control];
    return field.invalid && field.touched;
  }
}
