import { Component, computed, inject, signal } from '@angular/core';
import { HantDataService } from '../../core/hant-data.service';
import { DashboardStats, LeaderboardRow } from '../../core/models';
import { ViewportService } from '../../core/viewport.service';
import { DenToEurPipe } from '../../shared/den-to-eur.pipe';
import { SignedPipe } from '../../shared/signed.pipe';
import { DashboardMobileComponent } from '../mobile/dashboard/dashboard-mobile.component';

type SortKey = keyof Pick<LeaderboardRow, 'name' | 'wins' | 'losses' | 'netDen'>;
type SortDir = 'asc' | 'desc';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DenToEurPipe, SignedPipe, DashboardMobileComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  private readonly data = inject(HantDataService);
  readonly viewport = inject(ViewportService);

  readonly stats = signal<DashboardStats | null>(null);

  readonly columns: { key: SortKey; label: string }[] = [
    { key: 'name', label: 'Player' },
    { key: 'wins', label: 'Wins' },
    { key: 'losses', label: 'Losses' },
    { key: 'netDen', label: 'Net' }
  ];

  /** Most wins first, as the API already orders it. */
  readonly sort = signal<{ key: SortKey; dir: SortDir }>({ key: 'wins', dir: 'desc' });

  readonly leaderboard = computed(() => {
    const rows = this.stats()?.leaderboard ?? [];
    const { key, dir } = this.sort();
    const sign = dir === 'asc' ? 1 : -1;
    return [...rows].sort((a, b) =>
      key === 'name'
        ? sign * a.name.localeCompare(b.name)
        : sign * ((a[key] as number) - (b[key] as number))
    );
  });

  constructor() {
    this.data.getDashboard().subscribe((stats) => this.stats.set(stats));
  }

  /** A new column starts on its most useful end; the same one flips. */
  sortBy(key: SortKey): void {
    this.sort.update((current) =>
      current.key === key
        ? { key, dir: current.dir === 'asc' ? 'desc' : 'asc' }
        : { key, dir: key === 'name' ? 'asc' : 'desc' }
    );
  }

  sortDir(key: SortKey): SortDir | null {
    const current = this.sort();
    return current.key === key ? current.dir : null;
  }
}
