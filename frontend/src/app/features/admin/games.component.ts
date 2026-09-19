import { Component, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { formatDate, statusLabel } from '../../core/game-stats';
import { Game } from '../../core/models';
import { ViewportService } from '../../core/viewport.service';
import { SeatOrderComponent } from '../../shared/seat-order.component';
import { GamesMobileComponent } from '../mobile/games/games-mobile.component';
import { GamesStore } from './games.store';

@Component({
  selector: 'app-games',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, SeatOrderComponent, GamesMobileComponent],
  providers: [GamesStore],
  templateUrl: './games.component.html',
  styleUrl: './games.component.scss'
})
export class GamesComponent {
  private readonly store = inject(GamesStore);
  readonly viewport = inject(ViewportService);

  readonly games = this.store.games;
  readonly users = this.store.users;
  readonly showForm = this.store.showForm;
  readonly saving = this.store.saving;
  /** Player ids ticked for the new game. */
  readonly picked = this.store.picked;
  readonly form = this.store.form;

  readonly formatDate = formatDate;
  readonly status = statusLabel;

  openForm(): void {
    this.store.openForm();
  }

  cancel(): void {
    this.store.cancel();
  }

  togglePlayer(id: number): void {
    this.store.togglePlayer(id);
  }

  create(): void {
    this.store.create();
  }

  deleteGame(game: Game): void {
    this.store.deleteGame(game);
  }
}
