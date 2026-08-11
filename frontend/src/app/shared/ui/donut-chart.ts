import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export interface DonutSegment {
  label: string;
  value: number;
  color: string;
}

/**
 * Doughnut drawn as plain SVG.
 *
 * No chart library for two shapes: the whole thing is a handful of stroked arcs, and
 * pulling in a rendering engine to draw them would cost more bytes than the rest of the
 * application. Percentages add up because the buckets the API returns are mutually
 * exclusive by construction.
 */
@Component({
  selector: 'app-donut-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="donut">
      <div class="donut__figure">
        <svg viewBox="0 0 120 120" role="img" [attr.aria-label]="ariaLabel()">
          <circle class="donut__track" cx="60" cy="60" [attr.r]="radius" />
          @for (arc of arcs(); track arc.label) {
            <circle
              cx="60"
              cy="60"
              [attr.r]="radius"
              [attr.stroke]="arc.color"
              [attr.stroke-dasharray]="arc.dash"
              [attr.stroke-dashoffset]="arc.offset"
              class="donut__arc"
            />
          }
        </svg>
        <div class="donut__center">
          <strong>{{ total() }}</strong>
          <span>{{ centerLabel() }}</span>
        </div>
      </div>

      <ul class="legend">
        @for (arc of arcs(); track arc.label) {
          <li class="legend__item">
            <span class="legend__dot" [style.background]="arc.color"></span>
            <span class="legend__label">{{ arc.label }}</span>
            <span class="legend__value">{{ arc.value }}</span>
            <span class="legend__percent">{{ arc.percent }}%</span>
          </li>
        }
      </ul>
    </div>
  `,
  styles: `
    .donut {
      display: flex;
      align-items: center;
      gap: var(--space-8);
      flex-wrap: wrap;
    }

    .donut__figure {
      position: relative;
      width: 168px;
      height: 168px;
      flex-shrink: 0;
    }

    svg {
      width: 100%;
      height: 100%;
      transform: rotate(-90deg);
    }

    .donut__track {
      fill: none;
      stroke: var(--surface-sunken);
      stroke-width: 14;
    }

    .donut__arc {
      fill: none;
      stroke-width: 14;
      stroke-linecap: butt;
      transition: stroke-dasharray var(--duration-base) var(--ease-out);
    }

    .donut__center {
      position: absolute;
      inset: 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 1px;
    }

    .donut__center strong {
      font-size: var(--text-xl);
      font-weight: 700;
    }

    .donut__center span {
      font-size: var(--text-xs);
      color: var(--text-muted);
    }

    .legend {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: var(--space-3);
      flex: 1;
      min-width: 200px;
    }

    .legend__item {
      display: grid;
      grid-template-columns: 10px 1fr auto auto;
      align-items: center;
      gap: var(--space-3);
      font-size: var(--text-base);
    }

    .legend__dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
    }

    .legend__label {
      color: var(--text-body);
    }

    .legend__value {
      font-weight: 650;
      font-variant-numeric: tabular-nums;
    }

    .legend__percent {
      color: var(--text-muted);
      font-size: var(--text-sm);
      font-variant-numeric: tabular-nums;
      min-width: 42px;
      text-align: right;
    }
  `,
})
export class DonutChart {
  protected readonly radius = 50;

  readonly segments = input.required<DonutSegment[]>();
  readonly centerLabel = input('total');

  protected readonly total = computed(() =>
    this.segments().reduce((sum, segment) => sum + segment.value, 0),
  );

  protected readonly arcs = computed(() => {
    const circumference = 2 * Math.PI * this.radius;
    const total = this.total();
    let consumed = 0;

    return this.segments().map((segment) => {
      const fraction = total === 0 ? 0 : segment.value / total;
      const length = fraction * circumference;
      // Each arc starts where the previous one ended: a negative dash offset rotates it.
      const arc = {
        ...segment,
        dash: `${length} ${circumference - length}`,
        offset: -consumed,
        percent: total === 0 ? 0 : Math.round(fraction * 100),
      };
      consumed += length;
      return arc;
    });
  });

  protected readonly ariaLabel = computed(() =>
    this.segments()
      .map((segment) => `${segment.label}: ${segment.value}`)
      .join(', '),
  );
}
