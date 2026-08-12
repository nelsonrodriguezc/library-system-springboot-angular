import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../core/api/admin.service';
import { ToastService } from '../../core/ui/toast.service';
import { PageResponse, UserSummary } from '../../core/models/api.models';
import { Icon } from '../../shared/ui/icon';
import { Pagination } from '../../shared/ui/pagination';
import { StatusBadge } from '../../shared/ui/status-badge';
import { EmptyState, ErrorState, Skeleton } from '../../shared/ui/feedback';

type Tab = 'todos' | 'activos' | 'bloqueados';

@Component({
  selector: 'app-admin-users',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, FormsModule, Icon, Pagination, StatusBadge, EmptyState, ErrorState, Skeleton],
  templateUrl: './users.html',
  styleUrl: './users.scss',
})
export class AdminUsers {
  private readonly admin = inject(AdminService);
  private readonly toasts = inject(ToastService);

  protected readonly result = signal<PageResponse<UserSummary> | null>(null);
  protected readonly loading = signal(true);
  protected readonly failed = signal(false);
  protected readonly tab = signal<Tab>('todos');
  protected readonly search = signal('');
  protected readonly page = signal(0);
  protected readonly unblockingId = signal<number | null>(null);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.failed.set(false);

    const blocked = this.tab() === 'todos' ? undefined : this.tab() === 'bloqueados';

    this.admin.users(this.search(), blocked, this.page()).subscribe({
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

  protected selectTab(tab: Tab): void {
    this.tab.set(tab);
    this.page.set(0);
    this.load();
  }

  protected submitSearch(): void {
    this.page.set(0);
    this.load();
  }

  protected goToPage(page: number): void {
    this.page.set(page);
    this.load();
  }

  /** Restores borrowing rights before the block lapses on its own. */
  protected unblock(user: UserSummary): void {
    this.unblockingId.set(user.id);
    this.admin.unblock(user.id).subscribe({
      next: () => {
        this.unblockingId.set(null);
        this.toasts.success('Bloqueo levantado', `${user.name} ya puede volver a pedir préstamos.`);
        this.load();
      },
      error: () => this.unblockingId.set(null),
    });
  }

  protected initials(name: string): string {
    return name
      .split(/\s+/)
      .slice(0, 2)
      .map((word) => word[0]?.toUpperCase() ?? '')
      .join('');
  }
}
