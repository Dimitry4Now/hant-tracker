import { Component, DestroyRef, computed, effect, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth.service';
import { formatDate, standingsFor, statusLabel } from '../../../core/game-stats';
import { Game, User } from '../../../core/models';
import { ViewportService } from '../../../core/viewport.service';
import { dayMonth, groupByMonth, shortDate } from '../../../shared/mobile/dates';
import { IconComponent } from '../../../shared/mobile/icon.component';
import { queryState } from '../../../shared/mobile/query-state';

/** Analysis on phones: games by month, and one game's result at `?game=<id>`. */
@Component({
  selector: 'app-analysis-mobile',
  standalone: true,
  imports: [RouterLink, IconComponent],
  templateUrl: './analysis-mobile.component.html',
  styleUrl: './analysis-mobile.component.scss'
})
export class AnalysisMobileComponent {
  readonly auth = inject(AuthService);
  private readonly viewport = inject(ViewportService);

  readonly games = input.required<Game[]>();
  readonly users = input.required<User[]>();

  readonly gameParam = queryState('game');

  readonly months = computed(() => groupByMonth(this.games()));

  readonly index = computed(() => {
    const id = Number(this.gameParam.value());
    return this.gameParam.value() ? this.games().findIndex((game) => game.id === id) : -1;
  });
  readonly selected = computed(() => this.games()[this.index()] ?? null);
  readonly newer = computed(() => (this.index() > 0 ? this.games()[this.index() - 1] : null));
  readonly older = computed(() => (this.index() >= 0 ? this.games()[this.index() + 1] ?? null : null));

  readonly standings = computed(() => {
    const game = this.selected();
    return game ? standingsFor(game, this.users()) : [];
  });

  readonly formatDate = formatDate;
  readonly shortDate = shortDate;
  readonly status = statusLabel;

  constructor() {
    // A single game is a detail screen: no app bar or tab bar.
    effect(() => this.viewport.bare.set(this.gameParam.value() !== null), { allowSignalWrites: true });
    inject(DestroyRef).onDestroy(() => this.viewport.bare.set(false));
  }

  open(game: Game): void {
    this.gameParam.open(String(game.id));
  }

  /** Newer/older swap in place, so back still returns to the list. */
  step(game: Game): void {
    this.gameParam.replace(String(game.id));
    window.scrollTo(0, 0);
  }

  back(): void {
    this.gameParam.close();
  }

  outcome(game: Game): string {
    const leader = standingsFor(game, this.users())[0];
    if (!leader || !game.rounds.length) {
      return '';
    }
    return game.inProgress ? `${leader.name} leads` : `${leader.name} won`;
  }

  short(game: Game): string {
    const day = dayMonth(game.playedOn);
    return game.gamesThatDay > 1 ? `${day} · Game ${game.gameOfDay}` : day;
  }

  subtitle(game: Game): string {
    return game.gamesThatDay > 1
      ? `Game ${game.gameOfDay} of ${game.gamesThatDay} that day`
      : shortDate(game.playedOn);
  }
}
