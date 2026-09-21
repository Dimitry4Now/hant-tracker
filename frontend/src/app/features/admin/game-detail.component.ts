import { Component, computed, inject } from '@angular/core';
import { FormArray, FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { formatDate, gameLabel } from '../../core/game-stats';
import { Round, RoundEntry } from '../../core/models';
import { OUTCOMES, OUTCOME_LABELS } from '../../core/scoring';
import { ViewportService } from '../../core/viewport.service';
import { DenToEurPipe } from '../../shared/den-to-eur.pipe';
import { SeatOrderComponent } from '../../shared/seat-order.component';
import { SignedPipe } from '../../shared/signed.pipe';
import { GameDetailMobileComponent } from '../mobile/game/game-detail-mobile.component';
import { GameDetailStore } from './game-detail.store';

/** Desktop layout of a game's page; the state lives in GameDetailStore. */
@Component({
  selector: 'app-game-detail',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, DenToEurPipe, SignedPipe, SeatOrderComponent, GameDetailMobileComponent],
  providers: [GameDetailStore],
  templateUrl: './game-detail.component.html',
  styleUrl: './game-detail.component.scss'
})
export class GameDetailComponent {
  private readonly store = inject(GameDetailStore);
  readonly viewport = inject(ViewportService);

  readonly game = this.store.game;
  readonly users = this.store.users;
  readonly players = this.store.players;
  readonly runningTotals = this.store.runningTotals;
  readonly moneyForm = this.store.moneyForm;
  readonly roundForm = this.store.roundForm;
  readonly saved = this.store.saved;
  readonly roundError = this.store.roundError;
  readonly editingRound = this.store.editingRound;
  readonly editingGame = this.store.editingGame;
  readonly gameError = this.store.gameError;
  readonly picked = this.store.picked;
  readonly gameForm = this.store.gameForm;

  readonly outcomes = OUTCOMES;
  readonly outcomeLabels = OUTCOME_LABELS;
  readonly formatDate = formatDate;
  readonly gameLabel = gameLabel;

  /** Column layout depends on the player count, so it is built at runtime. */
  readonly roundsColumns = computed(
    () => `0.7fr repeat(${this.players().length}, 0.9fr) 2.2fr auto`
  );

  get entries(): FormArray {
    return this.store.entries;
  }

  get cumulative(): boolean {
    return this.store.cumulative;
  }

  get amounts(): FormArray<FormControl<number>> {
    return this.store.amounts;
  }

  previousTotal(playerId: number): number {
    return this.store.previousTotal(playerId);
  }

  previewPoints(index: number): number | null {
    return this.store.previewPoints(index);
  }

  ruleMismatch(index: number): number | null {
    return this.store.ruleMismatch(index);
  }

  pointsInRound(round: Round, playerId: number): RoundEntry | undefined {
    return this.store.pointsInRound(round, playerId);
  }

  saveMoney(): void {
    this.store.saveMoney();
  }

  saveRound(): void {
    this.store.saveRound();
  }

  editRound(round: Round, form: HTMLElement): void {
    this.store.editRound(round);
    form.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  cancelRoundEdit(): void {
    this.store.cancelRoundEdit();
  }

  deleteRound(round: Round): void {
    this.store.deleteRound(round);
  }

  openGameForm(): void {
    this.store.openGameForm();
  }

  togglePlayer(id: number): void {
    this.store.togglePlayer(id);
  }

  saveGame(): void {
    this.store.saveGame();
  }

  deleteGame(): void {
    this.store.deleteGame();
  }

  setInProgress(inProgress: boolean): void {
    this.store.setInProgress(inProgress);
  }
}
