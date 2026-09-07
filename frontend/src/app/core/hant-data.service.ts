import { Injectable, signal } from '@angular/core';
import { Observable, delay, of } from 'rxjs';
import { ACCOUNT_REQUESTS, DASHBOARD, GAMES, PUBLIC_STATS, USERS } from './mock-data';
import {
  AccountRequest,
  DashboardStats,
  Game,
  GameMoney,
  PublicStats,
  Round,
  User
} from './models';

/**
 * In-memory stand-in for the Spring Boot API. Every method returns an
 * Observable so swapping the bodies for HttpClient calls needs no changes
 * in the components.
 */
@Injectable({ providedIn: 'root' })
export class HantDataService {
  private readonly users = signal<User[]>(structuredClone(USERS));
  private readonly requests = signal<AccountRequest[]>(structuredClone(ACCOUNT_REQUESTS));
  private readonly games = signal<Game[]>(structuredClone(GAMES));

  // --- read ------------------------------------------------------------

  getPublicStats(): Observable<PublicStats> {
    return this.respond(PUBLIC_STATS);
  }

  getDashboard(): Observable<DashboardStats> {
    return this.respond(DASHBOARD);
  }

  /** Newest day first; within a day, in the order they were played. */
  getGames(): Observable<Game[]> {
    const sorted = [...this.games()].sort(
      (a, b) => b.playedOn.localeCompare(a.playedOn) || a.gameOfDay - b.gameOfDay
    );
    return this.respond(sorted);
  }

  getGame(id: number): Observable<Game | undefined> {
    return this.respond(this.games().find((g) => g.id === id));
  }

  getUsers(): Observable<User[]> {
    return this.respond(this.users());
  }

  getAccountRequests(): Observable<AccountRequest[]> {
    return this.respond(this.requests());
  }

  // --- write -----------------------------------------------------------

  /** Creates a game and returns it — several games can share a date. */
  createGame(input: {
    playedOn: string;
    playerIds: number[];
    note: string | null;
  }): Observable<Game> {
    const sameDay = this.games().filter((g) => g.playedOn === input.playedOn);
    const gamesThatDay = sameDay.length + 1;

    const game: Game = {
      id: Math.max(0, ...this.games().map((g) => g.id)) + 1,
      playedOn: input.playedOn,
      gameOfDay: gamesThatDay,
      gamesThatDay,
      inProgress: true,
      majstorska: false,
      note: input.note,
      playerIds: input.playerIds,
      rounds: [],
      money: []
    };

    // The other games that day now belong to a larger set.
    this.games.update((gs) => [
      game,
      ...gs.map((g) => (g.playedOn === input.playedOn ? { ...g, gamesThatDay } : g))
    ]);

    return this.respond(game);
  }

  createUser(input: {
    fullName: string;
    displayName: string;
    email: string;
    role: User['role'];
  }): Observable<User> {
    const user: User = {
      id: Math.max(0, ...this.users().map((u) => u.id)) + 1,
      fullName: input.fullName,
      displayName: input.displayName,
      email: input.email,
      role: input.role,
      status: 'ACTIVE',
      joinedOn: new Date().toISOString().slice(0, 10)
    };
    this.users.update((us) => [...us, user]);
    return this.respond(user);
  }

  approveRequest(id: number): Observable<void> {
    const request = this.requests().find((r) => r.id === id);
    if (request) {
      const nextId = Math.max(0, ...this.users().map((u) => u.id)) + 1;
      this.users.update((users) => [
        ...users,
        {
          id: nextId,
          fullName: request.fullName,
          displayName: request.fullName,
          email: request.email,
          role: 'USER',
          status: 'ACTIVE',
          joinedOn: new Date().toISOString().slice(0, 10)
        }
      ]);
    }
    this.requests.update((rs) => rs.filter((r) => r.id !== id));
    return this.respond(undefined as void);
  }

  rejectRequest(id: number): Observable<void> {
    this.requests.update((rs) => rs.filter((r) => r.id !== id));
    return this.respond(undefined as void);
  }

  setUserStatus(id: number, status: User['status']): Observable<void> {
    this.users.update((us) => us.map((u) => (u.id === id ? { ...u, status } : u)));
    return this.respond(undefined as void);
  }

  renameUser(id: number, displayName: string): Observable<void> {
    this.users.update((us) => us.map((u) => (u.id === id ? { ...u, displayName } : u)));
    return this.respond(undefined as void);
  }

  addRound(gameId: number, round: Omit<Round, 'id'>): Observable<void> {
    this.games.update((gs) =>
      gs.map((g) =>
        g.id === gameId
          ? {
              ...g,
              rounds: [...g.rounds, { ...round, id: Math.max(0, ...g.rounds.map((r) => r.id)) + 1 }]
            }
          : g
      )
    );
    return this.respond(undefined as void);
  }

  saveMoney(gameId: number, money: GameMoney[]): Observable<void> {
    this.games.update((gs) => gs.map((g) => (g.id === gameId ? { ...g, money } : g)));
    return this.respond(undefined as void);
  }

  private respond<T>(value: T): Observable<T> {
    // Small delay so loading states behave like they will against the API.
    return of(structuredClone(value)).pipe(delay(50));
  }
}
