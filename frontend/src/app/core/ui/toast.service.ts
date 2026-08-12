import { Injectable, signal } from '@angular/core';

export type ToastTone = 'success' | 'error' | 'info';

export interface Toast {
  id: number;
  tone: ToastTone;
  title: string;
  detail?: string;
}

/**
 * Transient feedback. Deliberately the only channel for it: a failure that is only
 * written to the console is a failure the reader never learns about, which is exactly
 * what "nada de tragarse un 500 en silencio" is about.
 */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private static readonly DISMISS_AFTER_MS = 5000;
  private nextId = 1;

  private readonly items = signal<Toast[]>([]);
  readonly toasts = this.items.asReadonly();

  success(title: string, detail?: string): void {
    this.push('success', title, detail);
  }

  error(title: string, detail?: string): void {
    this.push('error', title, detail);
  }

  info(title: string, detail?: string): void {
    this.push('info', title, detail);
  }

  dismiss(id: number): void {
    this.items.update((current) => current.filter((toast) => toast.id !== id));
  }

  private push(tone: ToastTone, title: string, detail?: string): void {
    const id = this.nextId++;
    this.items.update((current) => [...current, { id, tone, title, detail }]);
    setTimeout(() => this.dismiss(id), ToastService.DISMISS_AFTER_MS);
  }
}
