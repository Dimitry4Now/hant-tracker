import { Location } from '@angular/common';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { formatDate, standingsFor } from '../../../core/game-stats';
import { Round } from '../../../core/models';
import { useBareChrome } from '../../../core/viewport.service';
import { DenToEurPipe } from '../../../shared/den-to-eur.pipe';
import { IconComponent } from '../../../shared/mobile/icon.component';
import { queryState } from '../../../shared/mobile/query-state';
import { SeatListComponent } from '../../../shared/mobile/seat-list.component';
import { SheetComponent } from '../../../shared/mobile/sheet.component';
import { SignedPipe } from '../../../shared/signed.pipe';
import { GameDetailStore } from '../../admin/game-detail.store';
import { RoundFormMobileComponent } from './round-form-mobile.component';

type Tab = 'sheet' | 'money' | 'details';

const LIVE_PLACES = ['Leading', '2nd', '3rd', '4th', '5th', '6th'];

/** A game on a phone: result, running-totals sheet, money and details as tabs. */
@Component({
  selector: 'app-game-detail-mobile',
  standalone: true,
  imports: [ReactiveFormsModule, IconComponent, SheetComponent, SeatListComponent, SignedPipe, DenToEurPipe, RoundFormMobileComponent],
  templateUrl: './game-detail-mobile.component.html',
  styleUrl: './game-detail-mobile.component.scss'
})
export class GameDetailMobileComponent {
  readonly store = inject(GameDetailStore);
  private readonly router = inject(Router);
  private readonly location = inject(Location);

  readonly tabParam = queryState('tab');
  /** 'round' for a new round, or 'round-<id>' to edit one. */
  readonly sheet = queryState('sheet');

  readonly tabs: { id: Tab; label: string }[] = [
    { id: 'sheet', label: 'Sheet' },
    { id: 'money', label: 'Money' },
    { id: 'details', label: 'Details' }
  ];
  readonly tab = computed<Tab>(() => {
    const value = this.tabParam.value();
    return value === 'money' || value === 'details' ? value : 'sheet';
  });

  readonly formatDate = formatDate;

  /** Undefined while no round screen is up; null for a new round, else the round's id. */
  readonly roundScreen = computed(() => {
    const value = this.sheet.value();
    if (value === 'round') {
      return null;
    }
    const match = /^round-(\d+)$/.exec(value ?? '');
    return match ? Number(match[1]) : undefined;
  });

  /** The ⋯ menu — small enough not to need its own history entry. */
  readonly menuOpen = signal(false);

  /** Lowest total first — the sheet's header and the result card read from it. */
  readonly standings = computed(() => {
    const game = this.store.game();
    return game ? standingsFor(game, this.store.users()) : [];
  });

  readonly nextDealer = computed(() => {
    const game = this.store.game();
    if (!game) {
      return '';
    }
    const id = game.playerIds[game.rounds.length % game.playerIds.length];
    return this.store.players().find((p) => p.id === id)?.name ?? '';
  });

  constructor() {
    useBareChrome();

    // Details is the edit-game form, filled from the game each time it opens.
    effect(
      () => {
        const details = this.tab() === 'details';
        const loaded = !!this.store.game();
        untracked(() => {
          if (details && loaded && !this.store.editingGame()) {
            this.store.openGameForm();
          } else if (!details && this.store.editingGame()) {
            this.store.editingGame.set(false);
          }
        });
      },
      { allowSignalWrites: true }
    );
  }

  back(): void {
    // Angular numbers its navigations; anything past the first is in-app history.
    const navigationId = (this.location.getState() as { navigationId?: number } | null)?.navigationId ?? 0;
    if (navigationId > 1) {
      this.location.back();
    } else {
      void this.router.navigate(['/admin/games']);
    }
  }

  subtitle(): string {
    const game = this.store.game();
    if (!game) {
      return '';
    }
    const rounds = `${game.rounds.length} ${game.rounds.length === 1 ? 'round' : 'rounds'}`;
    if (game.inProgress) {
      return `In progress · ${rounds}`;
    }
    return game.gamesThatDay > 1 ? `Game ${game.gameOfDay} of ${game.gamesThatDay} that day` : rounds;
  }

  total(playerId: number): number {
    return this.standings().find((row) => row.playerId === playerId)?.points ?? 0;
  }

  /** Place under each name in the sheet's header. */
  place(playerId: number): string {
    const index = this.standings().findIndex((row) => row.playerId === playerId);
    if (index < 0 || !this.store.game()?.rounds.length) {
      return '';
    }
    return this.store.game()?.inProgress ? LIVE_PLACES[index] ?? '' : this.standings()[index].result;
  }

  leader(): number | null {
    return this.store.game()?.rounds.length ? this.standings()[0]?.playerId ?? null : null;
  }

  totalAfter(round: Round, playerId: number): number {
    return this.store.runningTotals().get(round.id)?.get(playerId) ?? 0;
  }

  points(round: Round, playerId: number): number {
    return this.store.pointsInRound(round, playerId)?.points ?? 0;
  }

  isWinner(round: Round, playerId: number): boolean {
    return this.store.pointsInRound(round, playerId)?.outcome === 'WINNER';
  }

  roundTag(round: Round): string {
    return round.majstorska ? 'Majst.' : round.hant ? 'Hant' : '';
  }

  setTab(tab: Tab): void {
    this.tabParam.replace(tab === 'sheet' ? null : tab);
  }

  addRound(): void {
    this.sheet.open('round');
  }

  editRound(round: Round): void {
    this.sheet.open(`round-${round.id}`);
  }

  moneyTotal(): number {
    return this.store.amounts.getRawValue().reduce((sum, amount) => sum + Number(amount ?? 0), 0);
  }

  editGame(): void {
    this.menuOpen.set(false);
    this.setTab('details');
  }

  deleteGame(): void {
    this.menuOpen.set(false);
    this.store.deleteGame(true);
  }

  saveGame(): void {
    this.store.saveGame(() => this.setTab('sheet'));
  }
}
