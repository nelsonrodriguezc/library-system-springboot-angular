import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthStore } from '../../core/auth/auth.store';
import { apiErrorMessage } from '../../core/http/error.interceptor';
import { Icon } from '../../shared/ui/icon';
import { AuthPanel } from './auth-panel';

@Component({
  selector: 'app-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, Icon, AuthPanel],
  templateUrl: './login.html',
  styleUrl: './auth-form.scss',
})
export class Login {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  /** Where to go after signing in, set by the guard that sent the reader here. */
  readonly volverA = input<string>();
  /** Present when the session was dropped rather than ended on purpose. */
  readonly expirada = input<string>();

  protected readonly submitting = signal(false);
  protected readonly failure = signal<string | null>(null);
  protected readonly passwordVisible = signal(false);

  protected readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
    remember: [true],
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const { email, password, remember } = this.form.getRawValue();
    this.submitting.set(true);
    this.failure.set(null);

    this.auth.login(email.trim(), password, remember).subscribe({
      next: () => {
        this.submitting.set(false);
        void this.router.navigateByUrl(this.volverA() ?? '/inicio');
      },
      error: (response: unknown) => {
        this.submitting.set(false);
        this.failure.set(apiErrorMessage(response, 'No pudimos iniciar sesión. Inténtalo nuevamente.'));
      },
    });
  }

  protected invalid(control: 'email' | 'password'): boolean {
    const field = this.form.controls[control];
    return field.invalid && field.touched;
  }
}
