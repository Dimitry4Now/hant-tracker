import { Component, computed, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { HantDataService } from '../../core/hant-data.service';
import { formatDate, gameLabel } from '../../core/game-stats';
import { Game, Round, RoundEntry, RoundOutcome, User } from '../../core/models';
import { OUTCOMES, OUTCOME_LABELS, pointsFor } from '../../core/scoring';
import { DenToEurPipe } from '../../shared/den-to-eur.pipe';
import { SignedPipe } from '../../shared/signed.pipe';

@Component({
  selector: 'app-game-detail',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, DenToEurPipe, SignedPipe],
  templateUrl: './game-detail.component.html',
  styleUrl: './game-detail.component.scss'
})
export class GameDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly data = inject(HantDataService);
  private readonly fb = inject(FormBuilder);

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

  /** Column layout depends on the player count, so it is built at runtime. */
  readonly roundsColumns = computed(() => `0.7fr repeat(${this.players().length}, 0.9fr) 2.2fr`);

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
    this.data.addRound(game.id, round).subscribe(() => this.reload(game.id));
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
      dealerId: game.playerIds[game.rounds.length % game.playerIds.length]
    });
  }
}
