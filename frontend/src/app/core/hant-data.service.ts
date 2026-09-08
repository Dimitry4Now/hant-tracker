import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of, throwError } from 'rxjs';
import { API_BASE } from './api.config';
import {
  AccountRequest,
  DashboardStats,
  Game,
  GameMoney,
  PublicStats,
  Round,
  User
} from './models';

/** Thin HTTP layer over the Spring Boot API. */
@Injectable({ providedIn: 'root' })
export class HantDataService {
  private readonly http = inject(HttpClient);

  // --- read ------------------------------------------------------------

  getPublicStats(): Observable<PublicStats> {
    return this.http.get<PublicStats>(`${API_BASE}/stats/public`);
  }

  getDashboard(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${API_BASE}/stats/dashboard`);
  }

  /** Newest day first; within a day, in the order they were played. */
  getGames(): Observable<Game[]> {
    return this.http.get<Game[]>(`${API_BASE}/games`);
  }

  getGame(id: number): Observable<Game | undefined> {
    return this.http.get<Game>(`${API_BASE}/games/${id}`).pipe(
      catchError((error: unknown) => {
        const notFound = error instanceof HttpErrorResponse && error.status === 404;
        return notFound ? of(undefined) : throwError(() => error);
      })
    );
  }

  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${API_BASE}/users`);
  }

  getAccountRequests(): Observable<AccountRequest[]> {
    return this.http.get<AccountRequest[]>(`${API_BASE}/account-requests`);
  }

  // --- write -----------------------------------------------------------

  /** Creates a game and returns it — several games can share a date. */
  createGame(input: {
    playedOn: string;
    playerIds: number[];
    note: string | null;
  }): Observable<Game> {
    return this.http.post<Game>(`${API_BASE}/games`, input);
  }

  createUser(input: {
    fullName: string;
    displayName: string;
    email: string;
    role: User['role'];
  }): Observable<User> {
    return this.http.post<User>(`${API_BASE}/users`, input);
  }

  approveRequest(id: number): Observable<void> {
    return this.http.post<void>(`${API_BASE}/account-requests/${id}/approve`, {}).pipe(toVoid());
  }

  rejectRequest(id: number): Observable<void> {
    return this.http.post<void>(`${API_BASE}/account-requests/${id}/reject`, {});
  }

  setUserStatus(id: number, status: User['status']): Observable<void> {
    return this.http.patch<void>(`${API_BASE}/users/${id}/status`, { status }).pipe(toVoid());
  }

  renameUser(id: number, displayName: string): Observable<void> {
    return this.http.patch<void>(`${API_BASE}/users/${id}/name`, { displayName }).pipe(toVoid());
  }

  /** Points are computed client-side by `scoring.ts` and stored as sent. */
  addRound(gameId: number, round: Omit<Round, 'id'>): Observable<void> {
    return this.http.post<Game>(`${API_BASE}/games/${gameId}/rounds`, round).pipe(toVoid());
  }

  saveMoney(gameId: number, money: GameMoney[]): Observable<void> {
    return this.http.put<Game>(`${API_BASE}/games/${gameId}/money`, { money }).pipe(toVoid());
  }
}

/** These endpoints answer with the updated entity; callers only need completion. */
function toVoid<T>() {
  return map<T, void>(() => undefined);
}
