import { Component, input, model } from '@angular/core';
import { User } from '../../core/models';
import { IconComponent } from './icon.component';

/**
 * Mobile take on SeatOrderComponent: seat 1 deals first and the deal moves
 * counterclockwise. One thumb-sized "move up" per row instead of up/down pairs.
 */
@Component({
  selector: 'app-seat-list',
  standalone: true,
  imports: [IconComponent],
  template: `
    <ol class="m-list">
      @for (id of seats(); track id; let i = $index; let first = $first) {
        <li class="seat">
          <span class="m-seat">{{ i + 1 }}</span>
          <span class="grow">{{ nameOf(id) }}</span>
          @if (first) {
            <span class="m-tag-accent">Deals first</span>
          } @else {
            <button type="button" class="m-icon-btn" (click)="moveUp(i)"
                    [attr.aria-label]="'Move ' + nameOf(id) + ' up'">
              <app-icon name="back" [size]="18" class="up" />
            </button>
          }
        </li>
      }
    </ol>
  `,
  styles: [
    `
      .seat {
        height: 56px;
        padding: 0 4px 0 12px;
        display: flex;
        align-items: center;
        gap: 10px;
        font-size: 15px;
      }

      .seat .m-tag-accent {
        margin-right: 10px;
      }

      .up {
        transform: rotate(90deg);
      }
    `
  ]
})
export class SeatListComponent {
  readonly seats = model.required<number[]>();
  readonly users = input.required<User[]>();

  nameOf(id: number): string {
    return this.users().find((u) => u.id === id)?.displayName ?? `Player ${id}`;
  }

  moveUp(index: number): void {
    this.seats.update((ids) => {
      const next = [...ids];
      [next[index - 1], next[index]] = [next[index], next[index - 1]];
      return next;
    });
  }
}
