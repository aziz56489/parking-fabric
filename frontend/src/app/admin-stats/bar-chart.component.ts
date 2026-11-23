import { Component, Input } from '@angular/core';

export interface BarPoint {
  label: string;
  value: number;
  /** Formatted value for the tooltip, axis and table, e.g. "12.500 TND". */
  display: string;
  /** Extra tooltip line, e.g. "8 exits". */
  detail?: string;
}

/**
 * One-series bar chart in plain HTML: thin bars with rounded tops on a shared baseline,
 * a recessive grid, a hover/focus tooltip, and a table view for screen readers and exact values.
 * One series, so no legend: the card title names it.
 */
@Component({
  selector: 'app-bar-chart',
  standalone: true,
  template: `
    <div class="toolbar">
      <button type="button" class="link" (click)="table = !table">{{ table ? 'Show chart' : 'Show table' }}</button>
    </div>

    @if (table) {
      <table class="data">
        <thead><tr><th>{{ labelHeader }}</th><th class="num">{{ valueHeader }}</th></tr></thead>
        <tbody>
          @for (p of points; track p.label) {
            <tr><td>{{ p.label }}</td><td class="num">{{ p.display }}@if (p.detail) { <span class="muted"> · {{ p.detail }}</span> }</td></tr>
          }
        </tbody>
      </table>
    } @else {
      <div class="chart" role="img" [attr.aria-label]="ariaLabel">
        <div class="y-axis">
          <span>{{ format(niceMax) }}</span>
          <span>{{ format(niceMax / 2) }}</span>
          <span>0</span>
        </div>
        <div class="plot" (mouseleave)="hovered = null">
          <div class="grid top"></div>
          <div class="grid mid"></div>
          <div class="bars">
            @for (p of points; track p.label; let i = $index) {
              <div class="slot" tabindex="0"
                   [attr.aria-label]="p.label + ': ' + p.display"
                   (mouseenter)="hovered = i" (focus)="hovered = i" (blur)="hovered = null">
                <div class="bar" [class.active]="hovered === i" [style.height.%]="height(p.value)"></div>
                @if (hovered === i) {
                  <div class="tooltip" [class.left]="i > points.length * 0.6" [class.right]="i < points.length * 0.25">
                    <strong>{{ p.display }}</strong>
                    <span>{{ p.label }}</span>
                    @if (p.detail) { <span class="muted">{{ p.detail }}</span> }
                  </div>
                }
              </div>
            }
          </div>
          <div class="x-axis">
            @for (p of points; track p.label; let i = $index) {
              <span [style.visibility]="i % labelStep === 0 || i === points.length - 1 ? 'visible' : 'hidden'">{{ p.label }}</span>
            }
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    :host { display: block; }
    .toolbar { display: flex; justify-content: flex-end; }
    .link { background: none; border: none; color: #4f46e5; font-weight: 600; padding: 4px 0; cursor: pointer; }
    .chart { display: flex; gap: 8px; height: 220px; margin-top: 4px; }
    .y-axis { display: flex; flex-direction: column; justify-content: space-between; padding-bottom: 22px;
              font-size: 11px; color: #64748b; text-align: right; min-width: 40px; }
    .plot { position: relative; flex: 1; display: flex; flex-direction: column; min-width: 0; }
    .grid { position: absolute; left: 0; right: 0; border-top: 1px solid #eef0f5; }
    .grid.top { top: 0; }
    .grid.mid { top: calc((100% - 22px) / 2); }
    .bars { position: relative; flex: 1; display: flex; align-items: flex-end; gap: 2px;
            border-bottom: 1px solid #cbd2e1; }
    .slot { position: relative; flex: 1; height: 100%; display: flex; align-items: flex-end; justify-content: center;
            cursor: default; outline: none; min-width: 0; }
    .slot:focus-visible { box-shadow: inset 0 0 0 2px #a5b4fc; border-radius: 4px; }
    .bar { width: 100%; max-width: 28px; background: #4f46e5; border-radius: 4px 4px 0 0; transition: height 0.4s ease; }
    .bar.active { background: #3730a3; }
    .tooltip { position: absolute; bottom: calc(100% + 6px); left: 50%; transform: translateX(-50%); z-index: 2;
               background: #0f172a; color: white; padding: 8px 10px; border-radius: 10px; font-size: 12px;
               display: flex; flex-direction: column; gap: 2px; white-space: nowrap; pointer-events: none;
               box-shadow: 0 6px 18px rgba(15, 23, 42, 0.25); }
    .tooltip.left { left: auto; right: 0; transform: none; }
    .tooltip.right { left: 0; transform: none; }
    .tooltip .muted { color: #cbd5e1; }
    .x-axis { display: flex; gap: 2px; height: 22px; align-items: flex-end; font-size: 11px; color: #64748b; }
    .x-axis span { flex: 1; text-align: center; white-space: nowrap; overflow: visible; min-width: 0; }
    .data { width: 100%; border-collapse: collapse; font-size: 13px; }
    .data th, .data td { padding: 6px 8px; border-bottom: 1px solid #eef0f5; text-align: left; }
    .num { text-align: right !important; font-variant-numeric: tabular-nums; }
    .muted { color: #64748b; }
  `],
})
export class BarChartComponent {
  @Input({ required: true }) points: BarPoint[] = [];
  @Input() labelHeader = 'Period';
  @Input() valueHeader = 'Value';
  @Input() ariaLabel = 'Bar chart';
  /** Formats axis values (e.g. money with currency decimals). */
  @Input() format: (value: number) => string = v => String(Math.round(v));

  hovered: number | null = null;
  table = false;

  /** A round top for the scale, so the axis reads 0 / half / max cleanly. */
  get niceMax() {
    const max = Math.max(0, ...this.points.map(p => p.value));
    if (max === 0) return 1;
    const magnitude = Math.pow(10, Math.floor(Math.log10(max)));
    const step = [1, 2, 2.5, 5, 10].find(s => s * magnitude >= max)!;
    return step * magnitude;
  }

  /** Show about 8 x labels at most. */
  get labelStep() {
    return Math.max(1, Math.ceil(this.points.length / 8));
  }

  height(value: number) {
    return (value / this.niceMax) * 100;
  }
}
