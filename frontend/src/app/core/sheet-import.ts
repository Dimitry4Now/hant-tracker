import { Round, SheetReading } from './models';
import { ScoreContext, guessOutcome, pointsFor } from './scoring';

/** Rounds a paper sheet can hold: 12 or 16 regular ones, with or without the Majstorska. */
export const SHEET_LENGTHS = [12, 13, 16, 17];

const WIN = pointsFor('WINNER', null, { majstorska: false, hant: false });
const HANT_WIN = pointsFor('WINNER', null, { majstorska: false, hant: true });

/** Running totals as on the paper: one row per round, one cell per sheet column. */
export type TotalsGrid = (number | null)[][];

/** One row of the sheet worked out into a round, with whatever does not add up. */
export interface SheetRound {
  number: number;
  majstorska: boolean;
  hant: boolean;
  /** Each column's points this round; null where a total is missing. */
  deltas: (number | null)[];
  /** Why a cell does not fit the rules, or null when it does. */
  problems: (string | null)[];
}

/** A sheet that ends on a 13th or 17th round ends on the Majstorska. */
export function endsInMajstorska(rounds: number): boolean {
  return rounds === 13 || rounds === 17;
}

/**
 * The round rows of a reading, each cut or padded to the sheet's columns.
 * Money rows the reader marked as rounds are dropped from the end: while the
 * count is not a sheet length, a last row read in full that is still no valid
 * round goes. A round with an unread cell stays, and so does a misread one
 * once the count is right.
 */
export function roundRows(
  reading: SheetReading,
  columns: number
): { totals: TotalsGrid; uncertain: boolean[][] } {
  const rows = reading.rows.filter((row) => row.kind === 'ROUND');
  const totals = rows.map((row) => fit(row.totals, columns, null));
  const uncertain = rows.map((row) => fit(row.uncertain, columns, false));
  while (totals.length > 0 && !SHEET_LENGTHS.includes(totals.length) && isNoRound(totals)) {
    totals.pop();
    uncertain.pop();
  }
  return { totals, uncertain };
}

/** Each row's points and what does not fit the rules, from the running totals. */
export function analyseSheet(totals: TotalsGrid, lastIsMajstorska: boolean): SheetRound[] {
  return totals.map((row, index) => {
    const deltas = deltasAt(totals, index);
    const majstorska = lastIsMajstorska && index === totals.length - 1;
    // Only a Hant pays the winner −150, so the totals say which rounds were one.
    const hant = majstorska || deltas.includes(HANT_WIN);
    const ctx: ScoreContext = { majstorska, hant, number: index + 1 };
    return { number: index + 1, majstorska, hant, deltas, problems: problemsIn(deltas, ctx) };
  });
}

/**
 * The rounds to store. Points are the change in each running total, outcomes
 * the best guess from them — as when a round is typed in from the sheet —
 * and dealers follow the seating, since the paper does not record them.
 */
export function toRounds(
  totals: TotalsGrid,
  columnPlayers: number[],
  seating: number[],
  lastIsMajstorska: boolean
): Omit<Round, 'id'>[] {
  return analyseSheet(totals, lastIsMajstorska).map((round, index) => {
    const ctx: ScoreContext = { majstorska: round.majstorska, hant: round.hant, number: round.number };
    return {
      number: round.number,
      dealerId: seating[index % seating.length],
      majstorska: round.majstorska,
      hant: round.hant,
      comment: null,
      entries: round.deltas.map((points, column) => {
        const value = points ?? 0;
        const outcome = guessOutcome(value, ctx);
        return {
          playerId: columnPlayers[column],
          outcome,
          cardValue: outcome === 'OPENED' ? value : null,
          points: value
        };
      })
    };
  });
}

/**
 * Which player each sheet column belongs to. A column goes to the player whose
 * name starts with the same letter as its label, Cyrillic or Latin; columns no
 * name matches take the players left over, in seating order.
 */
export function matchColumns(headers: string[], players: { id: number; name: string }[], columns: number): number[] {
  const left = players.map((player) => player.id);
  const matched: (number | null)[] = Array.from({ length: columns }, (_, column) => {
    const letter = initial(headers[column] ?? '');
    const player = players.find((p) => left.includes(p.id) && letter !== '' && initial(p.name) === letter);
    if (!player) {
      return null;
    }
    left.splice(left.indexOf(player.id), 1);
    return player.id;
  });
  return matched.map((id) => id ?? left.shift() ?? players[0]?.id ?? 0);
}

function fit<T>(cells: T[], length: number, filler: T): T[] {
  return Array.from({ length }, (_, index) => cells[index] ?? filler);
}

function deltasAt(totals: TotalsGrid, index: number): (number | null)[] {
  return totals[index].map((total, column) => {
    const before = index === 0 ? 0 : totals[index - 1][column];
    return total === null || before === null ? null : total - before;
  });
}

/** Whether the last row is clearly no round: read in full, yet not one winner taking −50 or −150. */
function isNoRound(totals: TotalsGrid): boolean {
  const deltas = deltasAt(totals, totals.length - 1);
  const winners = deltas.filter((delta) => delta !== null && delta < 0);
  return !deltas.includes(null) && !(winners.length === 1 && (winners[0] === WIN || winners[0] === HANT_WIN));
}

function problemsIn(deltas: (number | null)[], ctx: ScoreContext): (string | null)[] {
  const winners = deltas.filter((delta) => delta !== null && delta < 0).length;
  const complete = !deltas.includes(null);
  return deltas.map((delta) => {
    if (delta === null) {
      return 'Missing — fill in the total';
    }
    if (complete && winners === 0) {
      return 'Nobody won this round';
    }
    if (delta < 0) {
      const win = pointsFor('WINNER', null, ctx);
      if (winners > 1) {
        return 'More than one winner';
      }
      return delta === win ? null : `The winner takes ${win}`;
    }
    const outcome = guessOutcome(delta, ctx);
    const expected = pointsFor(outcome, delta, ctx);
    return expected === delta ? null : `The rules give ${expected}`;
  });
}

const LATIN: Record<string, string> = {
  а: 'a', б: 'b', в: 'v', г: 'g', ѓ: 'g', д: 'd', е: 'e', ж: 'z', з: 'z', ѕ: 'd', и: 'i', й: 'j', ј: 'j',
  к: 'k', ќ: 'k', л: 'l', љ: 'l', м: 'm', н: 'n', њ: 'n', о: 'o', п: 'p', р: 'r', с: 's', т: 't',
  у: 'u', ф: 'f', х: 'h', ц: 'c', ч: 'c', џ: 'd', ш: 's', щ: 's', ъ: 'a', ю: 'y', я: 'y'
};

/** A name's first letter in Latin, so "Д" on the sheet matches "Dimitar" in the app. */
function initial(name: string): string {
  const first = name.trim().charAt(0).toLowerCase();
  return LATIN[first] ?? first;
}
