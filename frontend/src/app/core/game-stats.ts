import { Game, User } from './models';

export interface PlayerStanding {
  playerId: number;
  name: string;
  points: number;
  /** 1-based position; lowest total points wins. */
  place: number;
  result: string;
}

const PLACE_LABELS = ['Winner', '2nd', '3rd', '4th', '5th', '6th'];

/** Sums every round for each player and ranks them — low score wins. */
export function standingsFor(game: Game, users: User[]): PlayerStanding[] {
  const nameOf = (id: number) => users.find((u) => u.id === id)?.displayName ?? `Player ${id}`;

  const totals = game.playerIds.map((playerId) => ({
    playerId,
    name: nameOf(playerId),
    points: game.rounds.reduce(
      (sum, round) => sum + (round.entries.find((e) => e.playerId === playerId)?.points ?? 0),
      0
    )
  }));

  return totals
    .sort((a, b) => a.points - b.points)
    .map((row, index) => ({
      ...row,
      place: index + 1,
      result: index === 0 && game.inProgress ? 'Leading' : PLACE_LABELS[index] ?? `${index + 1}th`
    }));
}

export function gameLabel(game: Game): string {
  const date = formatDate(game.playedOn);
  return game.gamesThatDay > 1 ? `${date} (Game ${game.gameOfDay})` : date;
}

export function statusLabel(game: Game): string {
  if (game.inProgress) {
    return 'In progress';
  }
  return game.majstorska ? 'Majstorska' : 'No Majstorska';
}

export function formatDate(iso: string): string {
  const date = new Date(`${iso}T00:00:00`);
  return date.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
}
