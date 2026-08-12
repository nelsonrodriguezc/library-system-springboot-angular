import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export interface LinePoint {
  label: string;
  total: number;
}

/**
 * Small trend line, drawn as plain SVG.
 *
 * <p>Two details keep it inside its card. The plot is a grid row with an explicit
 * {@code minmax(0, 1fr)} and the SVG is absolutely positioned inside it: a percentage
 * height on an SVG whose parent has no definite height resolves to the viewBox aspect
 * ratio instead, which for a square viewBox means a plot as tall as the card is wide.
 *
 * <p>The markers are HTML rather than {@code <circle>} elements. The path is stretched
 * with {@code preserveAspectRatio="none"}, which is fine for a line but would squash a
 * circle into an ellipse; positioning the dots in percentages keeps them round whatever
 * the card's proportions are.
 */
@Component({
  selector: 'app-line-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="chart">
      <!--
        Each label is pinned to its own grid line rather than spread with space-between,
        which aligns edges and would leave the outermost numbers about half a line off.
      -->
      <div class="chart__scale" aria-hidden="true">
        @for (tick of ticks(); track $index; let i = $index) {
          <span [style.top.%]="gridY(i)">{{ tick }}</span>
        }
      </div>

      <div class="chart__plot">
        <svg
          class="chart__canvas"
          viewBox="0 0 100 100"
          preserveAspectRatio="none"
          role="img"
          [attr.aria-label]="ariaLabel()"
        >
          @for (tick of ticks(); track $index; let i = $index) {
            <line
              class="chart__grid"
              x1="0"
              x2="100"
              [attr.y1]="gridY(i)"
              [attr.y2]="gridY(i)"
              vector-effect="non-scaling-stroke"
            />
          }

          <polygon class="chart__area" [attr.points]="areaPoints()" />
          <polyline
            class="chart__line"
            [attr.points]="linePoints()"
            vector-effect="non-scaling-stroke"
          />
        </svg>

        <!--
          Markers live outside the stretched coordinate system so they stay circular.
          The layer is inset to exactly the canvas box, which is what makes a percentage
          position land on the same spot as the polyline it belongs to.
        -->
        <div class="chart__markers">
          @for (point of plotted(); track point.label) {
            <span
              class="chart__dot"
              [style.left.%]="point.x"
              [style.top.%]="point.y"
              [title]="point.label + ': ' + point.total"
            ></span>
          }
        </div>
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
      grid-template-columns: 34px minmax(0, 1fr);
      /* minmax(0, 1fr) lets the plot row shrink instead of being pushed by its content. */
      grid-template-rows: minmax(0, 1fr) auto;
      gap: var(--space-2);
      height: 200px;
    }

    .chart__scale {
      position: relative;
      /* Matches the plot's padding so the percentages share one coordinate space. */
      margin: 7px 0;
      font-size: var(--text-xs);
      color: var(--text-faint);
      font-variant-numeric: tabular-nums;
    }

    .chart__scale span {
      position: absolute;
      right: 0;
      transform: translateY(-50%);
      line-height: 1;
      white-space: nowrap;
    }

    .chart__plot {
      position: relative;
      min-width: 0;
      min-height: 0;
      /* Room for the markers sitting on the top and bottom grid lines. */
      padding: 7px 0;
    }

    .chart__canvas {
      position: absolute;
      inset: 7px 0;
      width: 100%;
      height: calc(100% - 14px);
      display: block;
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

    .chart__markers {
      position: absolute;
      inset: 7px 0;
    }

    .chart__dot {
      position: absolute;
      width: 9px;
      height: 9px;
      border-radius: 50%;
      background: var(--surface-card);
      border: 2px solid var(--chart-line);
      transform: translate(-50%, -50%);
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

  protected gridY(index: number): number {
    const lines = this.ticks().length - 1;
    return lines === 0 ? 0 : (index / lines) * 100;
  }

  protected readonly linePoints = computed(() =>
    this.plotted()
      .map((point) => `${point.x},${point.y}`)
      .join(' '),
  );

  /** Closes the line down to the baseline so the area underneath can be tinted. */
  protected readonly areaPoints = computed(() => {
    if (this.plotted().length === 0) {
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
