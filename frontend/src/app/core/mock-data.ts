import {
  AccountRequest,
  DashboardStats,
  Game,
  PublicStats,
  Round,
  RoundEntry,
  RoundOutcome,
  User
} from './models';

export const USERS: User[] = [
  {
    id: 1,
    fullName: 'Player A',
    displayName: 'Player A',
    email: 'playera@example.com',
    role: 'ADMIN',
    status: 'ACTIVE',
    joinedOn: '2024-01-12'
  },
  {
    id: 2,
    fullName: 'Player B',
    displayName: 'Player B',
    email: 'playerb@example.com',
    role: 'USER',
    status: 'ACTIVE',
    joinedOn: '2024-01-12'
  },
  {
    id: 3,
    fullName: 'Player C',
    displayName: 'Player C',
    email: 'playerc@example.com',
    role: 'USER',
    status: 'ACTIVE',
    joinedOn: '2024-03-03'
  },
  {
    id: 4,
    fullName: 'Player D',
    displayName: 'Player D',
    email: 'playerd@example.com',
    role: 'USER',
    status: 'LOCKED',
    joinedOn: '2024-03-03'
  },
  {
    id: 5,
    fullName: 'Marko Petrovski',
    displayName: 'Marko P.',
    email: 'marko@example.com',
    role: 'USER',
    status: 'ACTIVE',
    joinedOn: '2026-08-24'
  }
];

export const ACCOUNT_REQUESTS: AccountRequest[] = [
  { id: 1, fullName: 'Marko P.', email: 'marko@example.com', requestedOn: '2026-09-05' },
  { id: 2, fullName: 'Ivana K.', email: 'ivana@example.com', requestedOn: '2026-09-07' },
  { id: 3, fullName: 'Petar S.', email: 'petar@example.com', requestedOn: '2026-08-31' }
];

/** Rounds of the 24 Aug 2026 game, transcribed from the GameDetail artboard. */
const GAME_1_ROUNDS: [number, number, number, number, string | null, boolean, boolean][] = [
  // [A, B, C, D, comment, hant, majstorska]
  [-50, 100, 100, 100, null, false, false],
  [100, -50, 70, 100, null, false, false],
  [100, 100, -50, 50, null, false, false],
  [-50, 80, 100, 100, null, false, false],
  [100, -50, 100, 100, 'Slow round, everyone folded early', false, false],
  [50, 100, -50, 100, null, false, false],
  [-50, 100, 60, 100, null, false, false],
  [100, 100, 100, -50, null, false, false],
  [-150, 140, 100, 100, 'First Hant of the day!', true, false],
  [100, -50, 100, 50, null, false, false],
  [90, 100, -50, 100, 'Close one, C sealed it late', false, false],
  [-50, 100, 100, 100, null, false, false],
  [100, 50, 100, -50, null, false, false],
  [100, 100, -50, 70, null, false, false],
  [-50, 100, 100, 100, null, false, false],
  [100, 100, -50, 100, null, false, false],
  [200, 200, -150, 200, 'Majstorska — C closes it out with a Hant!', true, true]
];

function outcomeFor(points: number): RoundOutcome {
  return points < 0 ? 'WINNER' : points >= 100 ? 'NOT_OPENED' : 'OPENED';
}

function buildRounds(
  rows: [number, number, number, number, string | null, boolean, boolean][],
  playerIds: number[]
): Round[] {
  return rows.map((row, index) => {
    const [a, b, c, d, comment, hant, majstorska] = row;
    const points = [a, b, c, d];
    const entries: RoundEntry[] = playerIds.map((playerId, i) => {
      const outcome = outcomeFor(points[i]);
      return {
        playerId,
        outcome,
        cardValue: outcome === 'OPENED' ? points[i] : null,
        points: points[i]
      };
    });
    return {
      id: index + 1,
      number: index + 1,
      dealerId: playerIds[index % playerIds.length],
      majstorska,
      hant,
      comment,
      entries
    };
  });
}

/** Deterministic filler so list screens have plausible round counts. */
function fillerRounds(count: number, playerIds: number[]): Round[] {
  const rows = Array.from({ length: count }, (_, i): [number, number, number, number, string | null, boolean, boolean] => {
    const winner = i % 4;
    const points = [100, 100, 100, 100];
    points[winner] = -50;
    return [points[0], points[1], points[2], points[3], null, false, false];
  });
  return buildRounds(rows, playerIds);
}

const CORE_PLAYERS = [1, 2, 3, 4];

export const GAMES: Game[] = [
  {
    id: 1,
    playedOn: '2026-08-24',
    gameOfDay: 1,
    gamesThatDay: 2,
    inProgress: false,
    majstorska: true,
    note: 'Great Majstorska finish — Player A closed it out with a clean Hant.',
    playerIds: CORE_PLAYERS,
    rounds: buildRounds(GAME_1_ROUNDS, CORE_PLAYERS),
    money: [
      { playerId: 1, amountDen: -50 },
      { playerId: 2, amountDen: -50 },
      { playerId: 3, amountDen: 150 },
      { playerId: 4, amountDen: -50 }
    ]
  },
  {
    id: 2,
    playedOn: '2026-08-24',
    gameOfDay: 2,
    gamesThatDay: 2,
    inProgress: false,
    majstorska: true,
    note: null,
    playerIds: CORE_PLAYERS,
    rounds: fillerRounds(9, CORE_PLAYERS),
    money: []
  },
  {
    id: 3,
    playedOn: '2026-08-17',
    gameOfDay: 1,
    gamesThatDay: 1,
    inProgress: false,
    majstorska: true,
    note: null,
    playerIds: CORE_PLAYERS,
    rounds: fillerRounds(17, CORE_PLAYERS),
    money: []
  },
  {
    id: 4,
    playedOn: '2026-08-10',
    gameOfDay: 1,
    gamesThatDay: 1,
    inProgress: true,
    majstorska: false,
    note: 'Rain delay, paused early.',
    playerIds: CORE_PLAYERS,
    rounds: fillerRounds(12, CORE_PLAYERS),
    money: []
  },
  {
    id: 5,
    playedOn: '2026-08-03',
    gameOfDay: 1,
    gamesThatDay: 1,
    inProgress: false,
    majstorska: true,
    note: null,
    playerIds: CORE_PLAYERS,
    rounds: fillerRounds(17, CORE_PLAYERS),
    money: []
  },
  {
    id: 6,
    playedOn: '2026-07-27',
    gameOfDay: 1,
    gamesThatDay: 1,
    inProgress: false,
    majstorska: false,
    note: null,
    playerIds: CORE_PLAYERS,
    rounds: fillerRounds(16, CORE_PLAYERS),
    money: []
  }
];

export const DASHBOARD: DashboardStats = {
  gamesThisMonth: 6,
  gamesLastMonth: 4,
  netDenThisMonth: 80,
  netDenLastMonth: -40,
  winRateThisMonth: 24,
  winRateLastMonth: 26,
  allTimeWinRate: 27,
  allTimeGames: 128,
  allTimeNetDen: 620,
  leaderboard: [
    { playerId: 1, name: 'Player A', points: 1240, wins: 32, losses: 11, netDen: 620 },
    { playerId: 2, name: 'Player B', points: 980, wins: 28, losses: 15, netDen: 180 },
    { playerId: 3, name: 'Player C', points: 640, wins: 19, losses: 24, netDen: -220 },
    { playerId: 4, name: 'Player D', points: 210, wins: 11, losses: 32, netDen: -580 }
  ]
};

export const PUBLIC_STATS: PublicStats = {
  startYear: 2019,
  gamesRange: 'Sep 2025 – Aug 2026',
  gamesByMonth: [
    { label: 'Sep', weeks: [1, 1, 2, 1, 0] },
    { label: 'Oct', weeks: [1, 2, 1, 1, 1] },
    { label: 'Nov', weeks: [1, 1, 1, 2, 0] },
    { label: 'Dec', weeks: [1, 0, 1, 1, 0] },
    { label: 'Jan', weeks: [1, 1, 1, 1, 1] },
    { label: 'Feb', weeks: [1, 1, 2, 1, 0] },
    { label: 'Mar', weeks: [2, 1, 1, 1, 0] },
    { label: 'Apr', weeks: [1, 1, 1, 2, 1] },
    { label: 'May', weeks: [1, 2, 1, 1, 0] },
    { label: 'Jun', weeks: [1, 1, 1, 1, 0] },
    { label: 'Jul', weeks: [1, 1, 2, 1, 0] },
    { label: 'Aug', weeks: [2, 1, 1, 2, 0] }
  ],
  gamesPlayed: 128,
  roundsRecorded: 2064,
  hantsRecorded: 187,
  players: 8,
  roundsEndingInHant: 9,
  roundsClosedRegular: 91,
  gamesWithQuitter: 14,
  rankings: [
    { name: 'Player A', games: 128, winRate: 31 },
    { name: 'Player B', games: 124, winRate: 28 },
    { name: 'Player C', games: 118, winRate: 24 },
    { name: 'Player D', games: 112, winRate: 21 },
    { name: 'Player E', games: 64, winRate: 27 },
    { name: 'Player F', games: 48, winRate: 22 },
    { name: 'Player G', games: 32, winRate: 19 },
    { name: 'Player H', games: 9, winRate: 11 }
  ],
  playstyles: [
    { name: 'Player A', style: 'Closer' },
    { name: 'Player B', style: 'Risk-taker' },
    { name: 'Player C', style: 'Cautious' },
    { name: 'Player D', style: 'Early exit' },
    { name: 'Player E', style: 'Steady' },
    { name: 'Player F', style: 'Wildcard' },
    { name: 'Player G', style: 'Grinder' },
    { name: 'Player H', style: 'Newcomer' }
  ]
};
