import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormControl, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { HantDataService } from '../../core/hant-data.service';
import { Game, Round, RoundEntry, RoundOutcome, User } from '../../core/models';
import { ScoreContext, guessOutcome as guessOutcomeFor, pointsFor } from '../../core/scoring';

/**
 * State behind a game's page — the sheet, the round form, money and the
 * edit-game form — shared by the desktop page and its mobile layout.
 * Provided per page, so it lives as long as it.
 */
@Injectable()
export class GameDetailStore {
  private readonly route = inject(ActivatedRoute);
  private readonly data = inject(HantDataService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  readonly game = signal<Game | null>(null);
  readonly users = signal<User[]>([]);

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

  readonly moneyForm = this.fb.nonNullable.group({
    amounts: this.fb.nonNullable.array<number>([])
  });

  readonly roundForm = this.fb.nonNullable.group({
    number: [1, [Validators.required, Validators.min(1)]],
    dealerId: [0, Validators.required],
    majstorska: [false],
    hant: [false],
    comment: [''],
    /** Enter each player's running total, as on the paper sheet, instead of the outcome. */
    cumulative: [true],
    entries: this.fb.array<ReturnType<FormBuilder['group']>>([])
  });

  readonly saved = signal<string | null>(null);
  readonly roundError = signal<string | null>(null);

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
    // Hant and Majstorska change what a not-opened or quit hand costs, and the
    // round number decides whether quitting is allowed, so the guessed outcomes
    // are redone when any of them changes.
    this.roundForm.controls.hant.valueChanges.subscribe(() => this.guessAllOutcomes());
    this.roundForm.controls.majstorska.valueChanges.subscribe(() => this.guessAllOutcomes());
    this.roundForm.controls.number.valueChanges.subscribe(() => this.guessAllOutcomes());

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

  /** Whether the round form takes running totals rather than outcomes. */
  get cumulative(): boolean {
    return this.roundForm.controls.cumulative.value;
  }

  get amounts(): FormArray<FormControl<number>> {
    return this.moneyForm.controls.amounts;
  }

  /**
   * A player's total before the round in the form: every round numbered lower,
   * leaving out the one being edited.
   */
  previousTotal(playerId: number): number {
    const number = this.roundForm.controls.number.value;
    const editingId = this.editingRound()?.id;
    return (this.game()?.rounds ?? [])
      .filter((round) => round.number < number && round.id !== editingId)
      .reduce((sum, round) => sum + (this.pointsInRound(round, playerId)?.points ?? 0), 0);
  }

  /**
   * Points for a row of the round form. With running totals it is the change
   * from the previous total, taken as written; otherwise the scoring rules
   * work it out from the outcome. Null while a total is still empty.
   */
  previewPoints(index: number): number | null {
    const entry = this.entryValue(index);
    if (this.roundForm.controls.cumulative.value) {
      return entry.total === null ? null : entry.total - this.previousTotal(entry.playerId);
    }
    return pointsFor(entry.outcome, entry.cardValue, this.scoreContext());
  }

  /**
   * What the rules would give for the chosen outcome, when a running total
   * says otherwise — usually a typo, or the wrong outcome picked. Null if they agree.
   */
  ruleMismatch(index: number): number | null {
    const entry = this.entryValue(index);
    const points = this.previewPoints(index);
    if (!this.roundForm.controls.cumulative.value || points === null || entry.outcome === 'OPENED') {
      return null;
    }
    const expected = pointsFor(entry.outcome, null, this.scoreContext());
    return expected === points ? null : expected;
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

  /** `done` runs once the round is stored — mobile closes its round screen then. */
  saveRound(done?: () => void): void {
    const game = this.game();
    if (!game || this.roundForm.invalid) {
      this.roundForm.markAllAsTouched();
      return;
    }
    const raw = this.roundForm.getRawValue();
    if (raw.cumulative && this.entries.controls.some((_, index) => this.previewPoints(index) === null)) {
      this.roundError.set("Enter every player's total.");
      return;
    }
    this.roundError.set(null);

    const round: Omit<Round, 'id'> = {
      number: raw.number,
      dealerId: raw.dealerId,
      majstorska: raw.majstorska,
      hant: raw.hant,
      comment: raw.comment.trim() || null,
      entries: this.entries.controls.map((_, index) => {
        const value = this.entryValue(index);
        const points = this.previewPoints(index) ?? 0;
        // From a running total, an opened hand's card value is just its points.
        const cardValue = raw.cumulative ? points : value.cardValue;
        return {
          playerId: value.playerId,
          outcome: value.outcome,
          cardValue: value.outcome === 'OPENED' ? cardValue : null,
          points
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
      done?.();
    });
  }

  /** Loads a round into the round form; saving then replaces it. */
  editRound(round: Round): void {
    this.editingRound.set(round);
    // Without events: the stored outcomes must not be replaced by guesses.
    this.roundForm.patchValue(
      {
        number: round.number,
        dealerId: round.dealerId,
        majstorska: round.majstorska,
        hant: round.hant,
        comment: round.comment ?? ''
      },
      { emitEvent: false }
    );
    this.entries.controls.forEach((control) => {
      const playerId = control.get('playerId')?.value as number;
      const entry = round.entries.find((e) => e.playerId === playerId);
      control.patchValue(
        {
          outcome: entry?.outcome ?? 'NOT_OPENED',
          cardValue: entry?.cardValue ?? null,
          total: this.previousTotal(playerId) + (entry?.points ?? 0)
        },
        { emitEvent: false }
      );
    });
  }

  cancelRoundEdit(): void {
    const game = this.game();
    this.editingRound.set(null);
    if (game) {
      this.buildForms(game);
    }
  }

  deleteRound(round: Round, done?: () => void): void {
    const game = this.game();
    if (!game || !confirm(`Delete round ${round.number}? Later rounds move up one.`)) {
      return;
    }
    this.data.deleteRound(game.id, round.id).subscribe(() => {
      if (this.editingRound()?.id === round.id) {
        this.editingRound.set(null);
      }
      this.reload(game.id);
      done?.();
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

  saveGame(done?: () => void): void {
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
          done?.();
        },
        error: (error: unknown) => {
          const conflict = error instanceof HttpErrorResponse && error.status === 409;
          this.gameError.set(
            conflict ? 'Players cannot change once the game has rounds.' : 'Could not save the game.'
          );
        }
      });
  }

  /** `replaceUrl` keeps the deleted game out of history — mobile uses it. */
  deleteGame(replaceUrl = false): void {
    const game = this.game();
    const rounds = game?.rounds.length ?? 0;
    if (!game || !confirm(`Delete this game and its ${rounds} rounds? This cannot be undone.`)) {
      return;
    }
    this.data.deleteGame(game.id).subscribe(() => void this.router.navigate(['/admin/games'], { replaceUrl }));
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
      const entry = this.fb.nonNullable.group({
        playerId: [playerId],
        outcome: ['NOT_OPENED' as RoundOutcome],
        cardValue: [null as number | null],
        /** Running total after this round, when entering from the paper sheet. */
        total: [null as number | null]
      });
      entry.controls.total.valueChanges.subscribe(() => this.guessOutcome(this.entries.controls.indexOf(entry)));
      this.entries.push(entry);
    }

    this.roundError.set(null);
    this.roundForm.patchValue(
      {
        number: game.rounds.length + 1,
        dealerId: game.playerIds[game.rounds.length % game.playerIds.length],
        majstorska: false,
        hant: false,
        comment: ''
      },
      { emitEvent: false }
    );
  }

  private entryValue(index: number) {
    return this.entries.at(index).getRawValue() as {
      playerId: number;
      outcome: RoundOutcome;
      cardValue: number | null;
      total: number | null;
    };
  }

  private scoreContext(): ScoreContext {
    const { majstorska, hant, number } = this.roundForm.getRawValue();
    return { majstorska, hant, number };
  }

  /** Pre-selects the outcome a running total points to; it can still be changed. */
  private guessOutcome(index: number): void {
    const points = this.previewPoints(index);
    if (index < 0 || !this.roundForm.controls.cumulative.value || points === null) {
      return;
    }
    this.entries.at(index).patchValue({ outcome: guessOutcomeFor(points, this.scoreContext()) }, { emitEvent: false });
  }

  private guessAllOutcomes(): void {
    this.entries.controls.forEach((_, index) => this.guessOutcome(index));
  }
}
