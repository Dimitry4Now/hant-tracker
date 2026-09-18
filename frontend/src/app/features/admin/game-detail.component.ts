import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { HantDataService } from '../../core/hant-data.service';
import { formatDate, gameLabel } from '../../core/game-stats';
import { Game, Round, RoundEntry, RoundOutcome, User } from '../../core/models';
import { OUTCOMES, OUTCOME_LABELS, pointsFor } from '../../core/scoring';
import { DenToEurPipe } from '../../shared/den-to-eur.pipe';
import { SeatOrderComponent } from '../../shared/seat-order.component';
import { SignedPipe } from '../../shared/signed.pipe';

@Component({
  selector: 'app-game-detail',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, DenToEurPipe, SignedPipe, SeatOrderComponent],
  templateUrl: './game-detail.component.html',
  styleUrl: './game-detail.component.scss'
})
export class GameDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly data = inject(HantDataService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  readonly game = signal<Game | null>(null);
  readonly users = signal<User[]>([]);

  readonly outcomes = OUTCOMES;
  readonly outcomeLabels = OUTCOME_LABELS;
  readonly formatDate = formatDate;
  readonly gameLabel = gameLabel;

  readonly players = computed(() => {
    const game = this.game();
    if (!game) {
      return [];
    }
    return game.playerIds.map((id) => ({
      id,
      name: this.users().find((u) => u.id === id)?.displayName ?? `Player ${id}`
    }));
  });

  /**
   * Each player's running score after every round, keyed by round id then
   * player id — the sheet shows totals, like the paper one, not per-round points.
   */
  readonly runningTotals = computed(() => {
    const totals = new Map<number, Map<number, number>>();
    const sums = new Map<number, number>();
    for (const round of this.game()?.rounds ?? []) {
      for (const entry of round.entries) {
        sums.set(entry.playerId, (sums.get(entry.playerId) ?? 0) + entry.points);
      }
      totals.set(round.id, new Map(sums));
    }
    return totals;
  });

  /** Column layout depends on the player count, so it is built at runtime. */
  readonly roundsColumns = computed(
    () => `0.7fr repeat(${this.players().length}, 0.9fr) 2.2fr auto`
  );

  readonly moneyForm = this.fb.nonNullable.group({
    amounts: this.fb.nonNullable.array<number>([])
  });

  readonly roundForm = this.fb.nonNullable.group({
    number: [1, [Validators.required, Validators.min(1)]],
    dealerId: [0, Validators.required],
    majstorska: [false],
    hant: [false],
    comment: [''],
    entries: this.fb.array<ReturnType<FormBuilder['group']>>([])
  });

  readonly saved = signal<string | null>(null);

  /** The round loaded into the round form, or null when it adds a new one. */
  readonly editingRound = signal<Round | null>(null);

  readonly editingGame = signal(false);
  readonly gameError = signal<string | null>(null);
  /** Player ids ticked in the edit-game form. */
  readonly picked = signal<number[]>([]);

  readonly gameForm = this.fb.nonNullable.group({
    playedOn: ['', Validators.required],
    note: ['']
  });

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    forkJoin({ game: this.data.getGame(id), users: this.data.getUsers() }).subscribe(
      ({ game, users }) => {
        this.users.set(users);
        this.game.set(game ?? null);
        if (game) {
          this.buildForms(game);
        }
      }
    );
  }

  get entries(): FormArray {
    return this.roundForm.get('entries') as FormArray;
  }

  get amounts(): FormArray<FormControl<number>> {
    return this.moneyForm.controls.amounts;
  }

  /** Points preview for a row of the add-round form. */
  previewPoints(index: number): number {
    const entry = this.entries.at(index).getRawValue() as {
      outcome: RoundOutcome;
      cardValue: number | null;
    };
    const { majstorska, hant } = this.roundForm.getRawValue();
    return pointsFor(entry.outcome, entry.cardValue, { majstorska, hant });
  }

  pointsInRound(round: Round, playerId: number): RoundEntry | undefined {
    return round.entries.find((e) => e.playerId === playerId);
  }

  moneyFor(playerId: number): number {
    return this.game()?.money.find((m) => m.playerId === playerId)?.amountDen ?? 0;
  }

  saveMoney(): void {
    const game = this.game();
    if (!game) {
      return;
    }
    const money = this.players().map((player, index) => ({
      playerId: player.id,
      amountDen: Number(this.amounts.at(index).value ?? 0)
    }));
    this.data.saveMoney(game.id, money).subscribe(() => {
      this.saved.set('Amounts saved.');
      this.reload(game.id);
    });
  }

  saveRound(): void {
    const game = this.game();
    if (!game || this.roundForm.invalid) {
      this.roundForm.markAllAsTouched();
      return;
    }
    const raw = this.roundForm.getRawValue();
    const round: Omit<Round, 'id'> = {
      number: raw.number,
      dealerId: raw.dealerId,
      majstorska: raw.majstorska,
      hant: raw.hant,
      comment: raw.comment.trim() || null,
      entries: this.entries.controls.map((control, index) => {
        const value = control.getRawValue() as { playerId: number; outcome: RoundOutcome; cardValue: number | null };
        return {
          playerId: value.playerId,
          outcome: value.outcome,
          cardValue: value.outcome === 'OPENED' ? value.cardValue : null,
          points: this.previewPoints(index)
        };
      })
    };
    const editing = this.editingRound();
    const request = editing
      ? this.data.updateRound(game.id, editing.id, round)
      : this.data.addRound(game.id, round);
    request.subscribe(() => {
      this.editingRound.set(null);
      this.reload(game.id);
    });
  }

  /** Loads a round into the round form; saving then replaces it. */
  editRound(round: Round, form: HTMLElement): void {
    this.editingRound.set(round);
    this.roundForm.patchValue({
      number: round.number,
      dealerId: round.dealerId,
      majstorska: round.majstorska,
      hant: round.hant,
      comment: round.comment ?? ''
    });
    this.entries.controls.forEach((control) => {
      const entry = round.entries.find((e) => e.playerId === control.get('playerId')?.value);
      control.patchValue({
        outcome: entry?.outcome ?? 'NOT_OPENED',
        cardValue: entry?.cardValue ?? null
      });
    });
    form.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  cancelRoundEdit(): void {
    const game = this.game();
    this.editingRound.set(null);
    if (game) {
      this.buildForms(game);
    }
  }

  deleteRound(round: Round): void {
    const game = this.game();
    if (!game || !confirm(`Delete round ${round.number}? Later rounds move up one.`)) {
      return;
    }
    this.data.deleteRound(game.id, round.id).subscribe(() => {
      if (this.editingRound()?.id === round.id) {
        this.editingRound.set(null);
      }
      this.reload(game.id);
    });
  }

  openGameForm(): void {
    const game = this.game();
    if (!game) {
      return;
    }
    this.gameForm.setValue({ playedOn: game.playedOn, note: game.note ?? '' });
    this.picked.set([...game.playerIds]);
    this.gameError.set(null);
    this.editingGame.set(true);
  }

  togglePlayer(id: number): void {
    this.picked.update((ids) =>
      ids.includes(id) ? ids.filter((existing) => existing !== id) : [...ids, id]
    );
  }

  saveGame(): void {
    const game = this.game();
    if (!game || this.gameForm.invalid || this.picked().length < 2) {
      this.gameForm.markAllAsTouched();
      return;
    }
    const { playedOn, note } = this.gameForm.getRawValue();
    this.data
      .updateGame(game.id, { playedOn, playerIds: this.picked(), note: note.trim() || null })
      .subscribe({
        next: () => {
          this.editingGame.set(false);
          this.reload(game.id);
        },
        error: (error: unknown) => {
          const conflict = error instanceof HttpErrorResponse && error.status === 409;
          this.gameError.set(
            conflict ? 'Players cannot change once the game has rounds.' : 'Could not save the game.'
          );
        }
      });
  }

  deleteGame(): void {
    const game = this.game();
    const rounds = game?.rounds.length ?? 0;
    if (!game || !confirm(`Delete this game and its ${rounds} rounds? This cannot be undone.`)) {
      return;
    }
    this.data.deleteGame(game.id).subscribe(() => void this.router.navigate(['/admin/games']));
  }

  private reload(id: number): void {
    this.data.getGame(id).subscribe((game) => {
      this.game.set(game ?? null);
      if (game) {
        this.buildForms(game);
      }
    });
  }

  private buildForms(game: Game): void {
    this.amounts.clear();
    this.entries.clear();

    for (const playerId of game.playerIds) {
      this.amounts.push(
        this.fb.nonNullable.control(
          game.money.find((m) => m.playerId === playerId)?.amountDen ?? 0
        )
      );
      this.entries.push(
        this.fb.nonNullable.group({
          playerId: [playerId],
          outcome: ['NOT_OPENED' as RoundOutcome],
          cardValue: [null as number | null]
        })
      );
    }

    this.roundForm.patchValue({
      number: game.rounds.length + 1,
      dealerId: game.playerIds[game.rounds.length % game.playerIds.length],
      majstorska: false,
      hant: false,
      comment: ''
    });
  }
}
