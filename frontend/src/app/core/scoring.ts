import { RoundOutcome } from './models';

export interface ScoreContext {
  majstorska: boolean;
  hant: boolean;
}

/**
 * Points for one player in one round.
 *
 * Low score wins, so the round winner takes a negative number. An "opened"
 * player scores the value of the cards left in hand; everyone else takes a
 * flat penalty, doubled in the Majstorska (final) round. A player who never
 * opened also pays double when the round ends in a Hant — but only once, so a
 * Majstorska Hant is still 200. A quitter pays 50, or 100 in a
 * Majstorska, a Hant or both.
 */
export function pointsFor(
  outcome: RoundOutcome,
  cardValue: number | null,
  ctx: ScoreContext
): number {
  switch (outcome) {
    case 'WINNER':
      return ctx.hant ? -150 : -50;
    case 'OPENED':
      return cardValue ?? 0;
    case 'NOT_OPENED':
      return ctx.majstorska || ctx.hant ? 200 : 100;
    case 'QUIT':
      return ctx.majstorska || ctx.hant ? 100 : 50;
  }
}

/**
 * Best guess at how a player finished, from the points they took in a round.
 * Used when rounds are entered as running totals off the paper sheet. It can
 * be wrong — an opened hand worth exactly a penalty reads as not opened or
 * quit — so the form lets
 * the outcome be changed.
 */
export function guessOutcome(points: number, ctx: ScoreContext): RoundOutcome {
  if (points < 0) {
    return 'WINNER';
  }
  if (points === pointsFor('NOT_OPENED', null, ctx)) {
    return 'NOT_OPENED';
  }
  if (points === pointsFor('QUIT', null, ctx)) {
    return 'QUIT';
  }
  return 'OPENED';
}

export const OUTCOME_LABELS: Record<RoundOutcome, string> = {
  WINNER: 'Winner',
  OPENED: 'Opened',
  NOT_OPENED: 'Not opened',
  QUIT: 'Quit'
};

export const OUTCOMES: RoundOutcome[] = ['WINNER', 'OPENED', 'NOT_OPENED', 'QUIT'];
