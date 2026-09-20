import { Game } from '../../core/models';

export interface MonthGroup {
  key: string;
  label: string;
  games: Game[];
}

function parse(iso: string): Date {
  return new Date(`${iso}T00:00:00`);
}

/** "September 2026" / "Sept 2026". */
export function monthLabel(iso: string, month: 'long' | 'short' = 'long'): string {
  return parse(iso).toLocaleDateString('en-GB', { month, year: 'numeric' });
}

/** "Thu 17 Sept". */
export function shortDate(iso: string): string {
  return parse(iso)
    .toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' })
    .replace(',', '');
}

/** "17 Sept". */
export function dayMonth(iso: string): string {
  return parse(iso).toLocaleDateString('en-GB', { day: 'numeric', month: 'short' });
}

export function weekday(iso: string): string {
  return parse(iso).toLocaleDateString('en-GB', { weekday: 'short' });
}

/** Groups games, already newest first, under their month. */
export function groupByMonth(games: Game[]): MonthGroup[] {
  const groups: MonthGroup[] = [];
  for (const game of games) {
    const key = game.playedOn.slice(0, 7);
    let group = groups[groups.length - 1];
    if (group?.key !== key) {
      group = { key, label: monthLabel(game.playedOn), games: [] };
      groups.push(group);
    }
    group.games.push(game);
  }
  return groups;
}
