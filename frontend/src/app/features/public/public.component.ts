import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HantDataService } from '../../core/hant-data.service';
import { PublicStats } from '../../core/models';
import { ThemeService } from '../../core/theme.service';
import { ViewportService } from '../../core/viewport.service';
import { BrandComponent } from '../../shared/brand.component';
import { IconComponent } from '../../shared/mobile/icon.component';
import { ThemeToggleComponent } from '../../shared/theme-toggle.component';

/** Height of the plot area in px; bar heights are derived from it. */
const PLOT_HEIGHT = 198;

interface Segment {
  height: number;
  color: string;
  radius: string;
}

interface MonthBar {
  label: string;
  total: number;
  segments: Segment[];
}

@Component({
  selector: 'app-public',
  standalone: true,
  imports: [RouterLink, BrandComponent, ThemeToggleComponent, IconComponent],
  templateUrl: './public.component.html',
  styleUrl: './public.component.scss',
  host: { '[class.mobile]': 'viewport.isMobile()' }
})
export class PublicComponent {
  readonly viewport = inject(ViewportService);
  readonly theme = inject(ThemeService);
  private readonly data = inject(HantDataService);

  readonly stats = signal<PublicStats | null>(null);

  readonly plotHeight = PLOT_HEIGHT;

  readonly legend = [1, 2, 3, 4, 5].map((week) => ({
    label: `Week ${week}`,
    color: `var(--week-${week})`
  }));

  /** Axis top, rounded up to an even number so the ticks land on whole games. */
  private readonly axisMax = computed(() => {
    const months = this.stats()?.gamesByMonth ?? [];
    const busiest = Math.max(0, ...months.map((m) => sum(m.weeks)));
    return Math.max(2, Math.ceil(busiest / 2) * 2);
  });

  readonly months = computed<MonthBar[]>(() => {
    const unit = PLOT_HEIGHT / this.axisMax();

    return (this.stats()?.gamesByMonth ?? []).map((month) => {
      const played = month.weeks
        .map((count, index) => ({ count, index }))
        .filter((week) => week.count > 0);

      // Rendered top-down, so the last week played sits on top and carries
      // the rounded end; the stack stays anchored square to the baseline.
      const segments = played
        .slice()
        .reverse()
        .map((week, position) => ({
          height: Math.round(week.count * unit) - 2,
          color: `var(--week-${week.index + 1})`,
          radius: position === 0 ? '4px 4px 0 0' : '0'
        }));

      return { label: month.label, total: sum(month.weeks), segments };
    });
  });

  readonly ticks = computed(() => {
    const max = this.axisMax();
    const unit = PLOT_HEIGHT / max;
    const values: number[] = [];
    for (let value = 0; value <= max; value += 2) {
      values.push(value);
    }
    return values.map((value) => ({ label: String(value), bottom: Math.round(value * unit) }));
  });

  /** Same positions as the ticks, minus the baseline the plot already draws. */
  readonly gridlines = computed(() => this.ticks().filter((tick) => tick.bottom > 0));

  constructor() {
    this.data.getPublicStats().subscribe((stats) => this.stats.set(stats));
  }
}

function sum(values: number[]): number {
  return values.reduce((total, value) => total + value, 0);
}
