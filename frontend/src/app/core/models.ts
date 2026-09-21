export type UserRole = 'ADMIN' | 'USER';
export type UserStatus = 'ACTIVE' | 'LOCKED';

export interface User {
  id: number;
  fullName: string;
  displayName: string;
  email: string;
  role: UserRole;
  status: UserStatus;
  joinedOn: string;
}

export interface AccountRequest {
  id: number;
  fullName: string;
  email: string;
  requestedOn: string;
}

/** How a player finished a single round. Drives the points column. */
export type RoundOutcome = 'WINNER' | 'OPENED' | 'NOT_OPENED' | 'QUIT';

export interface RoundEntry {
  playerId: number;
  outcome: RoundOutcome;
  /** Only meaningful for OPENED — the value of the cards left in hand. */
  cardValue: number | null;
  points: number;
}

export interface Round {
  id: number;
  number: number;
  dealerId: number;
  majstorska: boolean;
  hant: boolean;
  comment: string | null;
  entries: RoundEntry[];
}

export interface GameMoney {
  playerId: number;
  /** Whole denars, settled by hand. Euro is display-only. */
  amountDen: number;
}

export interface Game {
  id: number;
  playedOn: string;
  /** Nth game of that day — more than one game can share a date. */
  gameOfDay: number;
  gamesThatDay: number;
  inProgress: boolean;
  majstorska: boolean;
  note: string | null;
  playerIds: number[];
  rounds: Round[];
  money: GameMoney[];
}

export interface LeaderboardRow {
  playerId: number;
  name: string;
  wins: number;
  losses: number;
  netDen: number;
}

export interface DashboardStats {
  gamesThisMonth: number;
  gamesLastMonth: number;
  netDenThisMonth: number;
  netDenLastMonth: number;
  winRateThisMonth: number;
  winRateLastMonth: number;
  allTimeWinRate: number;
  allTimeGames: number;
  allTimeNetDen: number;
  leaderboard: LeaderboardRow[];
}

export interface MonthGames {
  label: string;
  /** Games played in each week of that month — a week holds 0–2 games. */
  weeks: number[];
}

export interface PublicStats {
  startYear: number;
  /** Human label for the span `gamesByMonth` covers, e.g. "Sep 2025 – Aug 2026". */
  gamesRange: string;
  gamesByMonth: MonthGames[];
  gamesPlayed: number;
  roundsRecorded: number;
  hantsRecorded: number;
  players: number;
  roundsEndingInHant: number;
  roundsClosedRegular: number;
  rankings: { name: string; games: number; winRate: number }[];
  playstyles: { name: string; style: string }[];
}
