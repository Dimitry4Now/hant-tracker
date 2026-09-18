import { Component, input, model } from '@angular/core';
import { User } from '../core/models';

/**
 * Orders the players around the table. Seat 1 deals first and the deal moves
 * counterclockwise, so this is also the column order on the score sheet.
 */
@Component({
  selector: 'app-seat-order',
  standalone: true,
  template: `
    <ol class="seats">
      @for (id of seats(); track id; let i = $index; let first = $first; let last = $last) {
        <li class="seat">
          <span class="number">{{ i + 1 }}</span>
          <span class="name">{{ nameOf(id) }}</span>
          @if (first) {
            <span class="badge badge-accent">First dealer</span>
          }
          <span class="moves">
            <button type="button" class="btn-ghost" [disabled]="first" (click)="move(i, -1)"
                    [attr.aria-label]="'Move ' + nameOf(id) + ' up'">↑</button>
            <button type="button" class="btn-ghost" [disabled]="last" (click)="move(i, 1)"
                    [attr.aria-label]="'Move ' + nameOf(id) + ' down'">↓</button>
          </span>
        </li>
      }
    </ol>
  `,
  styles: [
    `
      .seats {
        list-style: none;
        padding: 0;
        margin: 0;
        display: flex;
        flex-direction: column;
        gap: 6px;
        max-width: 420px;
      }

      .seat {
        display: flex;
        align-items: center;
        gap: 10px;
        height: 38px;
        padding: 0 8px 0 10px;
        border: 1px solid var(--border);
        border-radius: var(--radius);
        font-size: 13px;
      }

      .number {
        width: 18px;
        color: var(--muted);
        font-variant-numeric: tabular-nums;
      }

      .moves {
        margin-left: auto;
        display: flex;
        gap: 4px;
      }

      .moves .btn-ghost {
        width: 28px;
        justify-content: center;
        padding: 0;
      }

      .moves .btn-ghost:disabled {
        opacity: 0.35;
        cursor: default;
      }
    `
  ]
})
export class SeatOrderComponent {
  /** Player ids in seat order. */
  readonly seats = model.required<number[]>();
  readonly users = input.required<User[]>();

  nameOf(id: number): string {
    return this.users().find((u) => u.id === id)?.displayName ?? `Player ${id}`;
  }

  move(index: number, by: number): void {
    const seats = [...this.seats()];
    [seats[index], seats[index + by]] = [seats[index + by], seats[index]];
    this.seats.set(seats);
  }
}
