import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { HantDataService } from '../../core/hant-data.service';
import { formatDate, gameLabel, statusLabel } from '../../core/game-stats';
import { Game, User } from '../../core/models';

@Component({
  selector: 'app-games',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './games.component.html',
  styleUrl: './games.component.scss'
})
export class GamesComponent {
  private readonly data = inject(HantDataService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  readonly games = signal<Game[]>([]);
  readonly users = signal<User[]>([]);
  readonly showForm = signal(false);
  readonly saving = signal(false);
  /** Player ids ticked for the new game. */
  readonly picked = signal<number[]>([]);

  readonly formatDate = formatDate;
  readonly status = statusLabel;

  readonly form = this.fb.nonNullable.group({
    playedOn: [new Date().toISOString().slice(0, 10), Validators.required],
    note: ['']
  });

  constructor() {
    this.load();
  }

  openForm(): void {
    // Default to whoever played the most recent game.
    this.picked.set(this.games()[0]?.playerIds ?? []);
    this.form.patchValue({ playedOn: new Date().toISOString().slice(0, 10), note: '' });
    this.showForm.set(true);
  }

  cancel(): void {
    this.showForm.set(false);
  }

  togglePlayer(id: number): void {
    this.picked.update((ids) =>
      ids.includes(id) ? ids.filter((existing) => existing !== id) : [...ids, id]
    );
  }

  create(): void {
    if (this.form.invalid || this.picked().length < 2) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const { playedOn, note } = this.form.getRawValue();
    this.data
      .createGame({ playedOn, playerIds: this.picked(), note: note.trim() || null })
      .subscribe((game) => {
        this.saving.set(false);
        this.showForm.set(false);
        void this.router.navigate(['/admin/games', game.id]);
      });
  }

  deleteGame(game: Game): void {
    if (!confirm(`Delete the game from ${gameLabel(game)} and its ${game.rounds.length} rounds? This cannot be undone.`)) {
      return;
    }
    this.data.deleteGame(game.id).subscribe(() => this.load());
  }

  private load(): void {
    forkJoin({ games: this.data.getGames(), users: this.data.getUsers() }).subscribe(
      ({ games, users }) => {
        this.games.set(games);
        this.users.set(users);
      }
    );
  }
}
