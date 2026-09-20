import { Component, DestroyRef, computed, effect, inject, input, output, untracked } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { formatDate } from '../../../core/game-stats';
import { RoundOutcome } from '../../../core/models';
import { OUTCOMES, OUTCOME_LABELS } from '../../../core/scoring';
import { IconComponent } from '../../../shared/mobile/icon.component';
import { SignedPipe } from '../../../shared/signed.pipe';
import { GameDetailStore } from '../../admin/game-detail.store';

/** Full-screen round form on phones — adds a round, or edits one when given its id. */
@Component({
  selector: 'app-round-form-mobile',
  standalone: true,
  imports: [ReactiveFormsModule, IconComponent, SignedPipe],
  templateUrl: './round-form-mobile.component.html',
  styleUrl: './round-form-mobile.component.scss'
})
export class RoundFormMobileComponent {
  readonly store = inject(GameDetailStore);

  /** The round to edit; null adds the next one. */
  readonly roundId = input<number | null>(null);
  readonly closed = output();

  readonly outcomes = OUTCOMES;
  readonly outcomeLabels = OUTCOME_LABELS;
  readonly formatDate = formatDate;

  readonly editing = computed(() => {
    const id = this.roundId();
    return id === null ? null : this.store.game()?.rounds.find((round) => round.id === id) ?? null;
  });

  constructor() {
    // Loads the form once the game is in — on a reload the screen can open first.
    let ready = false;
    effect(
      () => {
        const game = this.store.game();
        if (!game || ready) {
          return;
        }
        ready = true;
        untracked(() => {
          const round = this.editing();
          if (round) {
            this.store.editRound(round);
          } else {
            this.store.cancelRoundEdit();
          }
        });
      },
      { allowSignalWrites: true }
    );

    document.body.style.overflow = 'hidden';
    inject(DestroyRef).onDestroy(() => {
      document.body.style.overflow = '';
      if (this.store.editingRound()) {
        this.store.cancelRoundEdit();
      }
    });
  }

  get cumulative(): boolean {
    return this.store.cumulative;
  }

  setCumulative(value: boolean): void {
    this.store.roundForm.controls.cumulative.setValue(value);
  }

  outcomeOf(index: number): RoundOutcome {
    return this.store.entries.at(index).get('outcome')?.value as RoundOutcome;
  }

  setOutcome(index: number, outcome: RoundOutcome): void {
    this.store.entries.at(index).get('outcome')?.setValue(outcome);
  }

  save(): void {
    this.store.saveRound(() => this.closed.emit());
  }

  delete(): void {
    const round = this.editing();
    if (round) {
      this.store.deleteRound(round, () => this.closed.emit());
    }
  }
}
