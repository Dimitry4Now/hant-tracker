import { HttpErrorResponse } from '@angular/common/http';
import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { Observable, switchMap } from 'rxjs';
import { formatDate } from '../../../core/game-stats';
import { HantDataService } from '../../../core/hant-data.service';
import { SheetReading } from '../../../core/models';
import {
  SHEET_LENGTHS,
  TotalsGrid,
  analyseSheet,
  endsInMajstorska,
  matchColumns,
  roundRows,
  toRounds
} from '../../../core/sheet-import';
import { downscaleImage } from '../../../shared/downscale-image';
import { GameDetailStore } from '../game-detail.store';

type Phase = 'pick' | 'reading' | 'review' | 'saving';

/**
 * Importing a game from a photo of its paper sheet: read it, let the admin
 * check the totals against the photo, then store every round at once.
 * Shared by the desktop panel and the mobile screen; it sits on top of the
 * page's GameDetailStore, which it reloads after saving.
 */
@Injectable()
export class SheetImportStore {
  private readonly data = inject(HantDataService);
  private readonly game = inject(GameDetailStore);

  readonly phase = signal<Phase>('pick');
  readonly error = signal<string | null>(null);
  /** The photo as picked, shown next to the grid to compare against. */
  readonly photoUrl = signal<string | null>(null);

  readonly headers = signal<string[]>([]);
  readonly sheetDate = signal<string | null>(null);
  readonly totals = signal<TotalsGrid>([]);
  readonly uncertain = signal<boolean[][]>([]);
  /** The player each sheet column belongs to. */
  readonly columns = signal<number[]>([]);
  readonly lastIsMajstorska = signal(false);
  readonly useSheetDate = signal(true);
  /** A sheet is imported once the game is over, so the game is closed by default. */
  readonly finished = signal(true);

  readonly rounds = computed(() => analyseSheet(this.totals(), this.lastIsMajstorska()));

  /** Shown above the grid; none of these stop a save. */
  readonly warnings = computed(() => {
    const warnings: string[] = [];
    const count = this.totals().length;
    if (count && !SHEET_LENGTHS.includes(count)) {
      warnings.push(`${count} rounds read — a sheet has 12 or 16, plus the Majstorska. Add or remove rows.`);
    }
    const problems = this.rounds().filter((round) => round.problems.some((p) => p !== null)).length;
    if (problems) {
      warnings.push(
        `${problems} ${problems === 1 ? 'round does' : 'rounds do'} not fit the rules — compare them with the photo.`
      );
    }
    return warnings;
  });

  /** Why the rounds cannot be saved yet, or null. Rule problems only warn. */
  readonly blocker = computed(() => {
    if (!this.totals().length) {
      return 'No rounds to save.';
    }
    if (this.totals().some((row) => row.includes(null))) {
      return 'Fill in every total first.';
    }
    if (new Set(this.columns()).size !== this.columns().length) {
      return 'Each column needs a different player.';
    }
    return null;
  });

  /** The date on the sheet, when it differs from the game's. */
  readonly otherDate = computed(() => {
    const date = this.sheetDate();
    return date && /^\d{4}-\d{2}-\d{2}$/.test(date) && date !== this.game.game()?.playedOn ? date : null;
  });

  readonly formatDate = formatDate;

  constructor() {
    inject(DestroyRef).onDestroy(() => this.setPhoto(null));
  }

  read(file: File): void {
    const game = this.game.game();
    if (!game) {
      return;
    }
    this.error.set(null);
    this.setPhoto(file);
    this.phase.set('reading');
    void downscaleImage(file).then((image) =>
      this.data.readSheet(game.id, image).subscribe({
        next: (reading) => this.load(reading),
        error: (error: unknown) => {
          this.phase.set('pick');
          this.error.set(readError(error));
        }
      })
    );
  }

  setTotal(row: number, column: number, value: string): void {
    const parsed = value.trim() === '' ? null : Number(value);
    this.totals.update((grid) =>
      grid.map((cells, r) =>
        r === row ? cells.map((cell, c) => (c === column ? (Number.isFinite(parsed) ? parsed : null) : cell)) : cells
      )
    );
    // A corrected cell has been checked, so it stops being marked as unsure.
    this.uncertain.update((grid) =>
      grid.map((cells, r) => (r === row ? cells.map((cell, c) => (c === column ? false : cell)) : cells))
    );
  }

  setColumn(column: number, playerId: number): void {
    this.columns.update((ids) => ids.map((id, c) => (c === column ? playerId : id)));
  }

  addRow(): void {
    const width = this.columns().length;
    this.totals.update((grid) => [...grid, Array(width).fill(null)]);
    this.uncertain.update((grid) => [...grid, Array(width).fill(false)]);
    this.lastIsMajstorska.set(endsInMajstorska(this.totals().length));
  }

  removeRow(row: number): void {
    this.totals.update((grid) => grid.filter((_, r) => r !== row));
    this.uncertain.update((grid) => grid.filter((_, r) => r !== row));
    this.lastIsMajstorska.set(endsInMajstorska(this.totals().length));
  }

  /** Back to picking a photo, dropping what was read. */
  restart(): void {
    this.setPhoto(null);
    this.totals.set([]);
    this.uncertain.set([]);
    this.error.set(null);
    this.phase.set('pick');
  }

  save(done?: () => void): void {
    const game = this.game.game();
    if (!game || this.blocker()) {
      return;
    }
    const rounds = toRounds(this.totals(), this.columns(), game.playerIds, this.lastIsMajstorska());
    const date = this.useSheetDate() ? this.otherDate() : null;
    const saveRounds = this.data.replaceRounds(game.id, rounds, this.finished());
    const request: Observable<void> = date
      ? this.data
          .updateGame(game.id, { playedOn: date, playerIds: game.playerIds, note: game.note })
          .pipe(switchMap(() => saveRounds))
      : saveRounds;

    this.error.set(null);
    this.phase.set('saving');
    request.subscribe({
      next: () => {
        this.game.reload(game.id);
        this.restart();
        done?.();
      },
      error: (error: unknown) => {
        this.phase.set('review');
        const conflict = error instanceof HttpErrorResponse && error.status === 409;
        this.error.set(conflict ? 'This game already has rounds.' : 'Could not save the rounds.');
      }
    });
  }

  private load(reading: SheetReading): void {
    const players = this.game.players();
    const { totals, uncertain } = roundRows(reading, players.length);
    this.headers.set(reading.headers);
    this.sheetDate.set(reading.date);
    this.totals.set(totals);
    this.uncertain.set(uncertain);
    this.columns.set(matchColumns(reading.headers, players, players.length));
    this.lastIsMajstorska.set(endsInMajstorska(totals.length));
    this.useSheetDate.set(true);
    this.finished.set(true);
    this.phase.set(totals.length ? 'review' : 'pick');
    if (!totals.length) {
      this.error.set('No rounds found on the photo. Try a sharper photo, taken straight on.');
    }
  }

  private setPhoto(file: File | null): void {
    const previous = this.photoUrl();
    if (previous) {
      URL.revokeObjectURL(previous);
    }
    this.photoUrl.set(file ? URL.createObjectURL(file) : null);
  }
}

function readError(error: unknown): string {
  const status = error instanceof HttpErrorResponse ? error.status : 0;
  switch (status) {
    case 503:
      return 'Reading sheets is not set up on the server yet (GEMINI_API_KEY).';
    case 429:
      return 'The free daily limit for reading sheets is used up. Try again tomorrow.';
    case 504:
      return 'Gemini is busy right now. Try again in a minute.';
    case 413:
      return 'The photo is too large to upload.';
    default:
      return 'Could not read the photo. Try a sharper photo, taken straight on.';
  }
}
