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
 * flat penalty, doubled in the Majstorska (final) round.
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
      return ctx.majstorska ? 200 : 100;
    case 'QUIT':
      return ctx.majstorska ? 200 : 50;
  }
}

export const OUTCOME_LABELS: Record<RoundOutcome, string> = {
  WINNER: 'Winner',
  OPENED: 'Opened',
  NOT_OPENED: 'Not opened',
  QUIT: 'Quit'
};

export const OUTCOMES: RoundOutcome[] = ['WINNER', 'OPENED', 'NOT_OPENED', 'QUIT'];
