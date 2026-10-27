import { SheetReading, SheetRow } from './models';
import { analyseSheet, endsInMajstorska, matchColumns, roundRows, toRounds } from './sheet-import';

/** The sheet of 18.08.2026 — columns Д Н А Л, 16 rounds and the Majstorska. */
const SHEET = [
  [200, -150, 200, 200],
  [210, -150, 200, 150],
  [310, -140, 150, 170],
  [260, -40, 250, 190],
  [110, 160, 450, 390],
  [200, 10, 650, 590],
  [250, 10, 600, 610],
  [200, 110, 630, 640],
  [230, 120, 580, 740],
  [430, 320, 430, 940],
  [470, 420, 530, 890],
  [480, 420, 480, 920],
  [680, 620, 600, 770],
  [780, 720, 620, 720],
  [980, 810, 820, 570],
  [930, 850, 920, 580],
  [1130, 700, 1120, 780]
];

/** The money worked out under the rounds on the same sheet. */
const MONEY = [
  [430, -430, -10, -350],
  [10, -420, 420, 90],
  [350, -80, 340, -340],
  [790, -930, 750, 600]
];

function row(totals: (number | null)[], kind: SheetRow['kind'] = 'ROUND'): SheetRow {
  return { kind, totals, uncertain: totals.map(() => false) };
}

function reading(rows: SheetRow[]): SheetReading {
  return { headers: ['Д', 'Н', 'А', 'Л'], date: '2026-08-18', rows };
}

describe('sheet import', () => {
  it('reads the sample sheet without a single problem', () => {
    const rounds = analyseSheet(SHEET, true);

    expect(rounds.flatMap((round) => round.problems).filter((p) => p !== null)).toEqual([]);
    expect(rounds.filter((round) => round.hant).map((round) => round.number)).toEqual([1, 5, 6, 10, 13, 15, 17]);
    expect(rounds.filter((round) => round.majstorska).map((round) => round.number)).toEqual([17]);
    expect(rounds[1].deltas).toEqual([10, 0, 0, -50]);
  });

  it('turns totals into rounds with outcomes, card values and dealers', () => {
    const rounds = toRounds(SHEET, [1, 2, 3, 4], [1, 2, 3, 4], true);

    expect(rounds).toHaveLength(17);
    expect(rounds[1].entries).toEqual([
      { playerId: 1, outcome: 'OPENED', cardValue: 10, points: 10 },
      { playerId: 2, outcome: 'OPENED', cardValue: 0, points: 0 },
      { playerId: 3, outcome: 'OPENED', cardValue: 0, points: 0 },
      { playerId: 4, outcome: 'WINNER', cardValue: null, points: -50 }
    ]);
    // Round 7: 50 points before round 13 reads as a quit.
    expect(rounds[6].entries[0].outcome).toBe('QUIT');
    expect(rounds[12].entries[2]).toEqual({ playerId: 3, outcome: 'OPENED', cardValue: 120, points: 120 });
    expect(rounds[16]).toMatchObject({ majstorska: true, hant: true });
    expect(rounds.map((round) => round.dealerId).slice(0, 6)).toEqual([1, 2, 3, 4, 1, 2]);
  });

  it('drops money rows read as rounds', () => {
    const rows = [...SHEET, ...MONEY].map((totals) => row(totals));

    expect(roundRows(reading(rows), 4).totals).toEqual(SHEET);
  });

  it('leaves out rows marked as money', () => {
    const rows = [...SHEET.map((totals) => row(totals)), ...MONEY.map((totals) => row(totals, 'MONEY'))];

    expect(roundRows(reading(rows), 4).totals).toHaveLength(17);
  });

  it('keeps a misread last round once the count is a sheet length', () => {
    // Round 16's 930 read as 980: nobody's total went down.
    const misread = [...SHEET.slice(0, 15), [980, 850, 920, 580]];

    const { totals } = roundRows(reading(misread.map((totals) => row(totals))), 4);

    expect(totals).toHaveLength(16);
    expect(analyseSheet(totals, false)[15].problems).toEqual(Array(4).fill('Nobody won this round'));
  });

  it('pads short rows and flags missing totals', () => {
    const { totals, uncertain } = roundRows(reading([row([200, -150, 200])]), 4);

    expect(totals).toEqual([[200, -150, 200, null]]);
    expect(uncertain).toEqual([[false, false, false, false]]);
    expect(analyseSheet(totals, false)[0].problems).toEqual([null, null, null, 'Missing — fill in the total']);
  });

  it('flags a misread cell against the rules', () => {
    const misread = SHEET.map((totals) => [...totals]);
    misread[13][0] = 80; // 780 read as +80: the 7's crossbar taken for a plus.

    const rounds = analyseSheet(misread, true);

    expect(rounds[13].problems).toEqual(['More than one winner', null, null, 'More than one winner']);
  });

  it('flags a Majstorska loser who did not pay 200', () => {
    const wrong = SHEET.map((totals) => [...totals]);
    wrong[16][0] = 1030;

    expect(analyseSheet(wrong, true)[16].problems[0]).toBe('The rules give 200');
    expect(analyseSheet(wrong, false)[16].problems[0]).toBeNull();
  });

  it('knows which sheet lengths end on the Majstorska', () => {
    expect([12, 13, 16, 17].map(endsInMajstorska)).toEqual([false, true, false, true]);
  });

  it('matches Cyrillic initials to players and leaves the rest in seating order', () => {
    const players = [
      { id: 1, name: 'Lazar' },
      { id: 2, name: 'Nikola' },
      { id: 3, name: 'Dimitar' },
      { id: 4, name: 'Bojan' }
    ];

    expect(matchColumns(['Д', 'Н', 'А', 'Л'], players, 4)).toEqual([3, 2, 4, 1]);
    expect(matchColumns([], players, 4)).toEqual([1, 2, 3, 4]);
  });
});
