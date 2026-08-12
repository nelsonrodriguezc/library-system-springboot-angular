import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Icon } from '../../shared/ui/icon';

/**
 * Split layout shared by signing in and signing up: the form on the left, an illustration
 * on the right.
 *
 * The artwork is drawn inline rather than shipped as a photograph — a few hundred bytes
 * of SVG instead of a few hundred kilobytes, and it scales to any screen without going
 * soft.
 */
@Component({
  selector: 'app-auth-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon],
  template: `
    <div class="auth">
      <section class="auth__form">
        <div class="auth__inner">
          <a class="brand" href="/">
            <span class="brand__mark"><app-icon name="library" [size]="22" /></span>
            <span class="brand__text">
              <strong>Libris</strong>
              <small>Library Management Platform</small>
            </span>
          </a>

          <ng-content />
        </div>

        <footer class="auth__footer">© 2026 Libris. Sistema de préstamos de biblioteca.</footer>
      </section>

      <aside class="auth__art" aria-hidden="true">
        <svg viewBox="0 0 400 560" preserveAspectRatio="xMidYMid slice">
          <defs>
            <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#1e2a5a" />
              <stop offset="100%" stop-color="#141c3d" />
            </linearGradient>
            <radialGradient id="glow" cx="50%" cy="18%" r="46%">
              <stop offset="0%" stop-color="#fbbf24" stop-opacity="0.34" />
              <stop offset="100%" stop-color="#fbbf24" stop-opacity="0" />
            </radialGradient>
          </defs>

          <rect width="400" height="560" fill="url(#sky)" />
          <rect width="400" height="560" fill="url(#glow)" />

          <!-- Hanging lamp -->
          <line x1="200" y1="0" x2="200" y2="74" stroke="#4a558c" stroke-width="2" />
          <path d="M172 74h56l14 26h-84z" fill="#fbbf24" opacity="0.92" />
          <circle cx="200" cy="106" r="5" fill="#fde68a" />

          <!-- Shelves -->
          <g>
            <rect x="52" y="196" width="296" height="6" rx="3" fill="#3b4676" />
            <rect x="52" y="316" width="296" height="6" rx="3" fill="#3b4676" />
            <rect x="52" y="436" width="296" height="6" rx="3" fill="#3b4676" />
          </g>

          <!-- Books, first shelf -->
          <g>
            <rect x="66" y="148" width="17" height="48" rx="2" fill="#6366f1" />
            <rect x="87" y="140" width="13" height="56" rx="2" fill="#a5b4fc" />
            <rect x="104" y="154" width="20" height="42" rx="2" fill="#f59e0b" />
            <rect x="128" y="144" width="15" height="52" rx="2" fill="#818cf8" />
            <rect x="147" y="158" width="18" height="38" rx="2" fill="#34d399" />
            <rect x="169" y="146" width="14" height="50" rx="2" fill="#e0e7ff" />
            <rect x="187" y="152" width="19" height="44" rx="2" fill="#f472b6" />
            <rect x="210" y="142" width="14" height="54" rx="2" fill="#6366f1" />
            <rect x="228" y="156" width="21" height="40" rx="2" fill="#fbbf24" />
            <rect x="253" y="148" width="15" height="48" rx="2" fill="#a5b4fc" />
            <rect x="272" y="150" width="18" height="46" rx="2" fill="#34d399" />
            <rect x="294" y="144" width="14" height="52" rx="2" fill="#e0e7ff" />
          </g>

          <!-- Books, second shelf -->
          <g>
            <rect x="70" y="270" width="19" height="46" rx="2" fill="#f472b6" />
            <rect x="93" y="262" width="14" height="54" rx="2" fill="#e0e7ff" />
            <rect x="111" y="276" width="17" height="40" rx="2" fill="#6366f1" />
            <rect x="132" y="266" width="20" height="50" rx="2" fill="#fbbf24" />
            <rect x="156" y="274" width="13" height="42" rx="2" fill="#34d399" />
            <rect x="173" y="264" width="18" height="52" rx="2" fill="#a5b4fc" />
            <rect x="195" y="272" width="15" height="44" rx="2" fill="#818cf8" />
            <rect x="214" y="268" width="19" height="48" rx="2" fill="#e0e7ff" />
            <rect x="237" y="278" width="14" height="38" rx="2" fill="#f59e0b" />
            <rect x="255" y="266" width="17" height="50" rx="2" fill="#6366f1" />
            <rect x="276" y="272" width="20" height="44" rx="2" fill="#f472b6" />
            <rect x="300" y="268" width="13" height="48" rx="2" fill="#a5b4fc" />
          </g>

          <!-- Reading desk -->
          <rect x="120" y="436" width="160" height="8" rx="3" fill="#4a558c" />
          <rect x="136" y="444" width="8" height="58" fill="#3b4676" />
          <rect x="256" y="444" width="8" height="58" fill="#3b4676" />

          <!-- Open book on the desk -->
          <path d="M164 436c14-10 28-10 36-4v-30c-8-6-22-6-36 4z" fill="#e0e7ff" />
          <path d="M236 436c-14-10-28-10-36-4v-30c8-6 22-6 36 4z" fill="#c7d2fe" />

          <!-- Desk lamp -->
          <path d="M268 436v-34h4v34z" fill="#3b4676" />
          <path d="M258 402h24l-6-16h-12z" fill="#fbbf24" />
        </svg>
      </aside>
    </div>
  `,
  styles: `
    .auth {
      display: grid;
      grid-template-columns: minmax(0, 1fr) minmax(0, 0.85fr);
      min-height: 100vh;
      background: var(--surface-card);
    }

    .auth__form {
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      padding: var(--space-10) var(--space-12);
    }

    .auth__inner {
      display: flex;
      flex-direction: column;
      gap: var(--space-8);
      max-width: 380px;
      width: 100%;
      margin: auto 0;
    }

    .brand {
      display: inline-flex;
      align-items: center;
      gap: var(--space-3);
      color: inherit;
      text-decoration: none;
      width: fit-content;
    }

    .brand:hover {
      text-decoration: none;
    }

    .brand__mark {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 42px;
      height: 42px;
      border-radius: var(--radius-md);
      background: var(--brand-navy);
      color: var(--text-on-dark);
    }

    .brand__text {
      display: flex;
      flex-direction: column;
      line-height: 1.2;
    }

    .brand__text strong {
      font-size: var(--text-xl);
      letter-spacing: -0.02em;
    }

    .brand__text small {
      font-size: var(--text-xs);
      color: var(--text-muted);
    }

    .auth__footer {
      font-size: var(--text-xs);
      color: var(--text-faint);
    }

    .auth__art {
      position: relative;
      overflow: hidden;
    }

    .auth__art svg {
      width: 100%;
      height: 100%;
      display: block;
    }

    @media (max-width: 900px) {
      .auth {
        grid-template-columns: 1fr;
      }

      .auth__art {
        display: none;
      }

      .auth__form {
        padding: var(--space-8) var(--space-5);
      }
    }
  `,
})
export class AuthPanel {}
