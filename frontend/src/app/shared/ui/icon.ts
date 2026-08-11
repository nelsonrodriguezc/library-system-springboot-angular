import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * Inline SVG icon drawn from the sprite embedded in index.html.
 *
 * A hand-picked set beats pulling in an icon library: no extra dependency, no font
 * download, and every glyph inherits `currentColor`, so an icon follows the colour of the
 * text it sits next to without any per-icon styling.
 *
 * <p>A sprite plus `<use>` rather than binding markup: Angular's sanitiser strips SVG
 * children out of `[innerHTML]`, and working around it would mean bypassing sanitisation
 * for no gain.
 */
export type IconName =
  | 'home'
  | 'library'
  | 'book'
  | 'bookmark'
  | 'users'
  | 'chart'
  | 'search'
  | 'plus'
  | 'check'
  | 'close'
  | 'chevron-left'
  | 'chevron-right'
  | 'chevron-down'
  | 'alert-circle'
  | 'alert-triangle'
  | 'info'
  | 'trash'
  | 'unlock'
  | 'lock'
  | 'logout'
  | 'calendar'
  | 'clock'
  | 'mail'
  | 'filter'
  | 'sparkles'
  | 'arrow-right'
  | 'refresh'
  | 'inbox'
  | 'eye'
  | 'eye-off'
  | 'user';

@Component({
  selector: 'app-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg
      [attr.width]="size()"
      [attr.height]="size()"
      viewBox="0 0 24 24"
      aria-hidden="true"
      focusable="false"
    >
      <use [attr.href]="reference()"></use>
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
      line-height: 0;
      flex-shrink: 0;
    }

    svg {
      fill: none;
      stroke: currentColor;
      stroke-width: 1.8;
      stroke-linecap: round;
      stroke-linejoin: round;
    }
  `,
})
export class Icon {
  readonly name = input.required<IconName>();
  readonly size = input(18);

  protected readonly reference = computed(() => `#i-${this.name()}`);
}
