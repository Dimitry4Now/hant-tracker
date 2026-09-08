import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { API_BASE } from './api.config';
import { User } from './models';

const STORAGE_KEY = 'hant-auth';

interface StoredSession {
  token: string;
  user: User;
}

interface LoginResponse {
  token: string;
  user: User;
}

export interface RegisterInput {
  fullName: string;
  displayName: string;
  email: string;
  password: string;
}

/** Holds the JWT the API issues and the account it belongs to. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly session = signal<StoredSession | null>(readStoredSession());

  readonly user = computed(() => this.session()?.user ?? null);
  readonly isLoggedIn = computed(() => this.session() !== null);
  readonly isAdmin = computed(() => this.session()?.user.role === 'ADMIN');

  get token(): string | null {
    return this.session()?.token ?? null;
  }

  /** Resolves to null on wrong credentials or a locked account. */
  login(email: string, password: string): Observable<User | null> {
    return this.http
      .post<LoginResponse>(`${API_BASE}/auth/login`, { email: email.trim(), password })
      .pipe(
        tap((response) => this.store(response)),
        map((response) => response.user),
        catchError(() => of(null))
      );
  }

  /** Sign-up lands in the admin queue — no account exists until it is approved. */
  register(input: RegisterInput): Observable<void> {
    return this.http.post<void>(`${API_BASE}/auth/register`, input);
  }

  logout(): void {
    this.session.set(null);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // ignore
    }
  }

  private store(session: StoredSession): void {
    this.session.set(session);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } catch {
      // ignore
    }
  }
}

function readStoredSession(): StoredSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as StoredSession) : null;
  } catch {
    return null;
  }
}
