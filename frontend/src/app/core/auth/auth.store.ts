import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, AuthenticatedProfile } from '../models/api.models';
import { TokenStorage } from './token-storage';

/**
 * Session state for the whole application, held in signals.
 *
 * Everything else reads {@link profile}, {@link isAuthenticated} and {@link isAdmin};
 * nothing else touches storage, so there is exactly one place that decides whether
 * somebody is signed in.
 */
@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly http = inject(HttpClient);
  private readonly storage = inject(TokenStorage);
  private readonly router = inject(Router);

  private readonly currentProfile = signal<AuthenticatedProfile | null>(this.storage.read()?.profile ?? null);

  readonly profile = this.currentProfile.asReadonly();
  readonly isAuthenticated = computed(() => this.currentProfile() !== null);
  readonly isAdmin = computed(() => this.currentProfile()?.role === 'ADMIN');
  readonly displayName = computed(() => this.currentProfile()?.name ?? '');
  readonly roleLabel = computed(() => (this.isAdmin() ? 'Administrador' : 'Bibliotecario'));

  /** True while the account is serving an overdue block. */
  readonly isBlocked = computed(() => {
    const blockedUntil = this.currentProfile()?.blockedUntil;
    return blockedUntil ? new Date(blockedUntil) > new Date() : false;
  });

  login(email: string, password: string, remember: boolean): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/login', { email, password })
      .pipe(tap((response) => this.startSession(response, remember)));
  }

  register(name: string, email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/register', { name, email, password })
      .pipe(tap((response) => this.startSession(response, true)));
  }

  logout(): void {
    this.storage.clear();
    this.currentProfile.set(null);
    void this.router.navigate(['/ingresar']);
  }

  /** Called by the error interceptor when the server stops accepting the token. */
  sessionExpired(): void {
    if (this.currentProfile() === null) {
      return;
    }
    this.storage.clear();
    this.currentProfile.set(null);
    void this.router.navigate(['/ingresar'], { queryParams: { expirada: 1 } });
  }

  private startSession(response: AuthResponse, remember: boolean): void {
    this.storage.save(response.token, response.user, remember);
    this.currentProfile.set(response.user);
  }
}
