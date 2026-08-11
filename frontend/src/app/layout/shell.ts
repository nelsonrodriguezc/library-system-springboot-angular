import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthStore } from '../core/auth/auth.store';
import { Icon, IconName } from '../shared/ui/icon';

interface NavEntry {
  label: string;
  path: string;
  icon: IconName;
}

@Component({
  selector: 'app-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, FormsModule, Icon],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthStore);

  protected readonly navigation: NavEntry[] = [
    { label: 'Inicio', path: '/inicio', icon: 'home' },
    { label: 'Catálogo', path: '/catalogo', icon: 'library' },
    { label: 'Mis préstamos', path: '/mis-prestamos', icon: 'book' },
    { label: 'Mis reservas', path: '/mis-reservas', icon: 'bookmark' },
  ];

  /**
   * Only an ADMIN sees this block. The mock-ups show it next to a "Bibliotecario"
   * account, but the rules give catalogue writes and global statistics to ADMIN alone, so
   * showing it to anyone else would be an invitation to a 403.
   */
  protected readonly administration: NavEntry[] = [
    { label: 'Resumen', path: '/admin/resumen', icon: 'chart' },
    { label: 'Usuarios', path: '/admin/usuarios', icon: 'users' },
    { label: 'Libros', path: '/admin/libros', icon: 'library' },
  ];

  protected readonly search = signal('');
  protected readonly menuOpen = signal(false);
  protected readonly sidebarOpen = signal(false);

  protected readonly initials = computed(() =>
    this.auth
      .displayName()
      .split(/\s+/)
      .slice(0, 2)
      .map((word) => word[0]?.toUpperCase() ?? '')
      .join(''),
  );

  /** The top bar search is a shortcut into the catalogue, not a second search engine. */
  protected submitSearch(): void {
    const term = this.search().trim();
    void this.router.navigate(['/catalogo'], { queryParams: term ? { q: term } : {} });
    this.sidebarOpen.set(false);
  }

  protected toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  protected closeMenu(): void {
    this.menuOpen.set(false);
  }

  protected toggleSidebar(): void {
    this.sidebarOpen.update((open) => !open);
  }

  protected logout(): void {
    this.closeMenu();
    this.auth.logout();
  }
}
