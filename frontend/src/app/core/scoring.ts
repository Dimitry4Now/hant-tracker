import { RoundOutcome } from './models';

export interface ScoreContext {
  majstorska: boolean;
  hant: boolean;
  /** The round's number, when known — it decides whether quitting is allowed. */
  number?: number;
}

/** A game has 16 regular rounds, and nobody may quit in the last four of them. */
export const LAST_ROUND_TO_QUIT = 12;

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
 * Outcomes the rules allow in a round. The Majstorska is played until someone
 * makes a Hant, so nobody quits or opens in it; and nobody quits in rounds 13
 * to 16, the last four regular ones.
 */
export function allowedOutcomes(ctx: ScoreContext): RoundOutcome[] {
  if (ctx.majstorska) {
    return ['WINNER', 'NOT_OPENED'];
  }
  if (ctx.number !== undefined && ctx.number > LAST_ROUND_TO_QUIT) {
    return ['WINNER', 'OPENED', 'NOT_OPENED'];
  }
  return OUTCOMES;
}

/**
 * Best guess at how a player finished, from the points they took in a round.
 * Used when rounds are entered as running totals off the paper sheet, and it
 * only picks outcomes the round allows. It can still be wrong — an opened hand
 * worth exactly a penalty reads as not opened or quit — so the form lets the
 * outcome be changed.
 */
export function guessOutcome(points: number, ctx: ScoreContext): RoundOutcome {
  const allowed = allowedOutcomes(ctx);
  if (points < 0) {
    return 'WINNER';
  }
  if (points === pointsFor('NOT_OPENED', null, ctx)) {
    return 'NOT_OPENED';
  }
  if (allowed.includes('QUIT') && points === pointsFor('QUIT', null, ctx)) {
    return 'QUIT';
  }
  // In the Majstorska every loser is "not opened"; a total that says otherwise
  // is a typo, which the form flags against the rules.
  return allowed.includes('OPENED') ? 'OPENED' : 'NOT_OPENED';
}

export const OUTCOME_LABELS: Record<RoundOutcome, string> = {
  WINNER: 'Winner',
  OPENED: 'Opened',
  NOT_OPENED: 'Not opened',
  QUIT: 'Quit'
};

export const OUTCOMES: RoundOutcome[] = ['WINNER', 'OPENED', 'NOT_OPENED', 'QUIT'];
