import { Component, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth.service';
import { HantDataService } from '../../../core/hant-data.service';
import { standingsFor } from '../../../core/game-stats';
import { DashboardStats, Game, User } from '../../../core/models';
import { DenToEurPipe } from '../../../shared/den-to-eur.pipe';
import { dayMonth } from '../../../shared/mobile/dates';
import { IconComponent } from '../../../shared/mobile/icon.component';
import { SignedPipe } from '../../../shared/signed.pipe';

const PLACES = ['leading', '2nd', '3rd', '4th', '5th', '6th'];

@Component({
  selector: 'app-dashboard-mobile',
  standalone: true,
  imports: [RouterLink, DenToEurPipe, SignedPipe, IconComponent],
  templateUrl: './dashboard-mobile.component.html',
  styleUrl: './dashboard-mobile.component.scss'
})
export class DashboardMobileComponent {
  private readonly data = inject(HantDataService);
  readonly auth = inject(AuthService);

  readonly stats = input.required<DashboardStats>();

  private readonly games = signal<Game[]>([]);
  private readonly users = signal<User[]>([]);

  readonly thisMonth = new Date().toLocaleDateString('en-GB', { month: 'long' });
  readonly lastMonth = new Date(new Date().getFullYear(), new Date().getMonth() - 1, 1).toLocaleDateString(
    'en-GB',
    { month: 'long' }
  );

  readonly live = computed(() => this.games().find((game) => game.inProgress) ?? null);

  /** Where the game in progress opens — only admins reach the sheet itself. */
  readonly liveLink = computed(() => {
    const game = this.live();
    if (!game) {
      return null;
    }
    return this.auth.isAdmin()
      ? { path: ['/admin/games', game.id], query: {} }
      : { path: ['/analysis'], query: { game: game.id } };
  });

  /** "17 Sept · round 12 · you're leading on 290". */
  readonly liveLine = computed(() => {
    const game = this.live();
    if (!game) {
      return '';
    }
    const parts = [dayMonth(game.playedOn), `round ${game.rounds.length}`];
    const standings = standingsFor(game, this.users());
    const me = this.auth.user()?.id;
    const mine = standings.findIndex((row) => row.playerId === me);
    if (game.rounds.length && mine >= 0) {
      parts.push(`you're ${PLACES[mine] ?? `${mine + 1}th`} on ${standings[mine].points}`);
    } else if (game.rounds.length && standings[0]) {
      parts.push(`${standings[0].name} leading on ${standings[0].points}`);
    }
    return parts.join(' · ');
  });

  constructor() {
    this.data.getGames().subscribe((games) => this.games.set(games));
    this.data.getUsers().subscribe((users) => this.users.set(users));
  }

  firstName(): string {
    return this.auth.user()?.displayName ?? '';
  }
}
