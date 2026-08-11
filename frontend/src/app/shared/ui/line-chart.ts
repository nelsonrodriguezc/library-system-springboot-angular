import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export interface LinePoint {
  label: string;
  total: number;
}

/**
 * Small trend line, again as plain SVG.
 *
 * Uses a fixed viewBox with `preserveAspectRatio="none"` so the plot stretches to the
 * card while the labels, drawn outside the SVG in HTML, keep their real font size.
 */
@Component({
  selector: 'app-line-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="chart">
      <div class="chart__scale" aria-hidden="true">
        @for (tick of ticks(); track tick) {
          <span>{{ tick }}</span>
        }
      </div>

      <div class="chart__plot">
        <svg
          viewBox="0 0 100 100"
          preserveAspectRatio="none"
          role="img"
          [attr.aria-label]="ariaLabel()"
        >
          @for (tick of ticks(); track tick; let i = $index) {
            <line
              class="chart__grid"
              x1="0"
              x2="100"
              [attr.y1]="(i / (ticks().length - 1)) * 100"
              [attr.y2]="(i / (ticks().length - 1)) * 100"
              vector-effect="non-scaling-stroke"
            />
          }

          <polygon class="chart__area" [attr.points]="areaPoints()" />
          <polyline class="chart__line" [attr.points]="linePoints()" vector-effect="non-scaling-stroke" />

          @for (point of plotted(); track point.label) {
            <circle
              class="chart__dot"
              [attr.cx]="point.x"
              [attr.cy]="point.y"
              r="3"
              vector-effect="non-scaling-stroke"
            >
              <title>{{ point.label }}: {{ point.total }}</title>
            </circle>
          }
        </svg>
      </div>

      <div class="chart__labels" aria-hidden="true">
        @for (point of points(); track point.label) {
          <span>{{ point.label }}</span>
        }
      </div>
    </div>
  `,
  styles: `
    .chart {
      display: grid;
      grid-template-columns: 34px 1fr;
      grid-template-rows: 1fr auto;
      gap: var(--space-2);
      height: 200px;
    }

    .chart__scale {
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      align-items: flex-end;
      font-size: var(--text-xs);
      color: var(--text-faint);
      font-variant-numeric: tabular-nums;
      padding-bottom: 2px;
    }

    .chart__plot {
      position: relative;
      min-width: 0;
    }

    svg {
      width: 100%;
      height: 100%;
      overflow: visible;
    }

    .chart__grid {
      stroke: var(--border-subtle);
      stroke-width: 1;
    }

    .chart__area {
      fill: var(--brand-indigo);
      opacity: 0.08;
    }

    .chart__line {
      fill: none;
      stroke: var(--chart-line);
      stroke-width: 2;
      stroke-linejoin: round;
      stroke-linecap: round;
    }

    .chart__dot {
      fill: var(--surface-card);
      stroke: var(--chart-line);
      stroke-width: 2;
    }

    .chart__labels {
      grid-column: 2;
      display: flex;
      justify-content: space-between;
      font-size: var(--text-xs);
      color: var(--text-muted);
    }
  `,
})
export class LineChart {
  readonly points = input.required<LinePoint[]>();

  /** Upper bound of the axis, rounded to something a person would choose. */
  private readonly ceiling = computed(() => {
    const highest = Math.max(...this.points().map((point) => point.total), 0);
    if (highest === 0) {
      return 4;
    }
    const step = Math.pow(10, Math.floor(Math.log10(highest)));
    return Math.ceil(highest / step) * step;
  });

  protected readonly ticks = computed(() => {
    const max = this.ceiling();
    return [4, 3, 2, 1, 0].map((level) => Math.round((max * level) / 4));
  });

  protected readonly plotted = computed(() => {
    const points = this.points();
    const max = this.ceiling();
    const lastIndex = Math.max(points.length - 1, 1);

    return points.map((point, index) => ({
      ...point,
      x: (index / lastIndex) * 100,
      y: 100 - (point.total / max) * 100,
    }));
  });

  protected readonly linePoints = computed(() =>
    this.plotted()
      .map((point) => `${point.x},${point.y}`)
      .join(' '),
  );

  /** Closes the line down to the baseline so the area underneath can be tinted. */
  protected readonly areaPoints = computed(() => {
    const plotted = this.plotted();
    if (plotted.length === 0) {
      return '';
    }
    return `0,100 ${this.linePoints()} 100,100`;
  });

  protected readonly ariaLabel = computed(() =>
    this.points()
      .map((point) => `${point.label}: ${point.total}`)
      .join(', '),
  );
}
