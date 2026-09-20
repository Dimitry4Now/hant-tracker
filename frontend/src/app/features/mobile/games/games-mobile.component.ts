import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth.service';
import { standingsFor, statusLabel } from '../../../core/game-stats';
import { Game } from '../../../core/models';
import { groupByMonth, monthLabel, shortDate, weekday } from '../../../shared/mobile/dates';
import { IconComponent } from '../../../shared/mobile/icon.component';
import { queryState } from '../../../shared/mobile/query-state';
import { SeatListComponent } from '../../../shared/mobile/seat-list.component';
import { SheetComponent } from '../../../shared/mobile/sheet.component';
import { GamesStore } from '../../admin/games.store';

type Filter = 'all' | 'mine' | 'no-majstorska' | 'notes';

/** Months shown before "Show <month>" is needed. */
const INITIAL_MONTHS = 2;

@Component({
  selector: 'app-games-mobile',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, IconComponent, SheetComponent, SeatListComponent],
  templateUrl: './games-mobile.component.html',
  styleUrl: './games-mobile.component.scss'
})
export class GamesMobileComponent {
  readonly store = inject(GamesStore);
  private readonly auth = inject(AuthService);

  readonly sheet = queryState('sheet');

  readonly filters: { id: Filter; label: string }[] = [
    { id: 'all', label: 'All' },
    { id: 'mine', label: 'I played' },
    { id: 'no-majstorska', label: 'No Majstorska' },
    { id: 'notes', label: 'With notes' }
  ];
  readonly filter = signal<Filter>('all');
  readonly monthsShown = signal(INITIAL_MONTHS);

  /** The newest unfinished game, pinned above the list. */
  readonly live = computed(() => this.store.games().find((game) => game.inProgress) ?? null);

  readonly liveStandings = computed(() => {
    const game = this.live();
    return game ? standingsFor(game, this.store.users()) : [];
  });

  readonly since = computed(() => {
    const games = this.store.games();
    const oldest = games[games.length - 1];
    return oldest ? monthLabel(oldest.playedOn, 'short') : '';
  });

  private readonly filtered = computed(() => {
    const me = this.auth.user()?.id;
    const games = this.store.games();
    switch (this.filter()) {
      case 'mine':
        return games.filter((game) => me !== undefined && game.playerIds.includes(me));
      case 'no-majstorska':
        return games.filter((game) => !game.inProgress && !game.majstorska);
      case 'notes':
        return games.filter((game) => !!game.note);
      default:
        return games;
    }
  });

  private readonly months = computed(() => groupByMonth(this.filtered()));

  readonly visibleMonths = computed(() => this.months().slice(0, this.monthsShown()));
  readonly nextMonth = computed(() => this.months()[this.monthsShown()] ?? null);

  readonly status = statusLabel;

  constructor() {
    // The sheet can open from the URL (a reload, or back/forward), so the form
    // is filled whenever it shows rather than only from the button.
    effect(
      () => {
        const open = this.sheet.value() === 'new-game';
        const loaded = this.store.games().length > 0;
        untracked(() => {
          if (open && loaded && !this.store.showForm()) {
            this.store.openForm();
          } else if (!open && this.store.showForm()) {
            this.store.cancel();
          }
        });
      },
      { allowSignalWrites: true }
    );
  }

  setFilter(filter: Filter): void {
    this.filter.set(filter);
    this.monthsShown.set(INITIAL_MONTHS);
  }

  showMore(): void {
    this.monthsShown.update((n) => n + 1);
  }

  openNewGame(): void {
    this.sheet.open('new-game');
  }

  /** "Player C won" / "Player A leading". */
  title(game: Game): string {
    const leader = standingsFor(game, this.store.users())[0];
    if (!leader || !game.rounds.length) {
      return 'Not started';
    }
    return game.inProgress ? `${leader.name} leading` : `${leader.name} won`;
  }

  day(game: Game): string {
    return String(Number(game.playedOn.slice(8, 10)));
  }

  weekday(game: Game): string {
    return weekday(game.playedOn);
  }

  shortDate(game: Game): string {
    return shortDate(game.playedOn);
  }

  create(): void {
    this.store.create(true);
  }
}
