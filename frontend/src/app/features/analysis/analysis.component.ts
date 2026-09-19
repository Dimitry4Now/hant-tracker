import { Component, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { HantDataService } from '../../core/hant-data.service';
import { gameLabel, standingsFor, statusLabel } from '../../core/game-stats';
import { Game, User } from '../../core/models';
import { ViewportService } from '../../core/viewport.service';
import { SignedPipe } from '../../shared/signed.pipe';
import { AnalysisMobileComponent } from '../mobile/analysis/analysis-mobile.component';

@Component({
  selector: 'app-analysis',
  standalone: true,
  imports: [SignedPipe, AnalysisMobileComponent],
  templateUrl: './analysis.component.html',
  styleUrl: './analysis.component.scss'
})
export class AnalysisComponent {
  private readonly data = inject(HantDataService);
  readonly viewport = inject(ViewportService);

  readonly games = signal<Game[]>([]);
  readonly users = signal<User[]>([]);
  readonly selectedId = signal<number | null>(null);

  readonly selected = computed(() => this.games().find((g) => g.id === this.selectedId()) ?? null);

  readonly standings = computed(() => {
    const game = this.selected();
    return game ? standingsFor(game, this.users()) : [];
  });

  readonly label = gameLabel;
  readonly status = statusLabel;

  constructor() {
    forkJoin({ games: this.data.getGames(), users: this.data.getUsers() }).subscribe(
      ({ games, users }) => {
        this.games.set(games);
        this.users.set(users);
        this.selectedId.set(games[0]?.id ?? null);
      }
    );
  }

  select(id: number): void {
    this.selectedId.set(id);
  }
}
