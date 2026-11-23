import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, shareReplay, tap } from 'rxjs';
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

/** What the login page can offer; email resets need SMTP on the server. */
export interface AuthOptions {
  passwordResetByEmail: boolean;
}

/** Why a login failed, for the message the page shows. */
export type LoginFailure = 'credentials' | 'locked' | 'too-many' | 'unavailable';

export type LoginResult = { user: User } | { failure: LoginFailure };

export interface RegisterInput {
  fullName: string;
  displayName: string;
  email: string;
  password: string;
}

export interface ProfileInput {
  displayName: string;
  email: string;
  currentPassword?: string;
}

/** Holds the JWT the API issues and the account it belongs to. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly session = signal<StoredSession | null>(readStoredSession());

  readonly user = computed(() => this.session()?.user ?? null);
  readonly isLoggedIn = computed(() => this.session() !== null);
  readonly isAdmin = computed(() => this.session()?.user.role === 'ADMIN');
  /** A step the player must take before the rest of the app opens up. */
  readonly requiredStep = computed(() => this.session()?.user.requiredStep ?? null);

  /** Asked once per page load; resets by email are off when the API can't be reached. */
  readonly options$: Observable<AuthOptions> = this.http
    .get<AuthOptions>(`${API_BASE}/auth/options`)
    .pipe(
      catchError(() => of({ passwordResetByEmail: false })),
      shareReplay(1)
    );

  get token(): string | null {
    return this.session()?.token ?? null;
  }

  login(email: string, password: string): Observable<LoginResult> {
    return this.http
      .post<LoginResponse>(`${API_BASE}/auth/login`, { email: email.trim(), password })
      .pipe(
        tap((response) => this.store(response)),
        map((response): LoginResult => ({ user: response.user })),
        catchError((error: unknown) => of<LoginResult>({ failure: loginFailure(error) }))
      );
  }

  /** Sign-up lands in the admin queue — no account exists until it is approved. */
  register(input: RegisterInput): Observable<void> {
    return this.http.post<void>(`${API_BASE}/auth/register`, input);
  }

  /** `currentPassword` is only checked by the API when the email changes. */
  updateProfile(input: ProfileInput): Observable<User> {
    return this.http
      .put<User>(`${API_BASE}/account/profile`, input)
      .pipe(tap((user) => this.updateUser(user)));
  }

  /** The change signs out every session; the answer carries a new token for this one. */
  changePassword(currentPassword: string, newPassword: string): Observable<User> {
    return this.http
      .put<LoginResponse>(`${API_BASE}/account/password`, { currentPassword, newPassword })
      .pipe(
        tap((response) => this.store(response)),
        map((response) => response.user)
      );
  }

  /** Emails a code to the address; it becomes the account's once confirmed. */
  sendEmailCode(email: string): Observable<void> {
    return this.http.post<void>(`${API_BASE}/account/email`, { email: email.trim() });
  }

  confirmEmailCode(code: string): Observable<User> {
    return this.http
      .post<User>(`${API_BASE}/account/email/confirm`, { code: code.trim() })
      .pipe(tap((user) => this.updateUser(user)));
  }

  /** The API answers the same whether or not the email has an account. */
  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>(`${API_BASE}/auth/forgot-password`, { email: email.trim() });
  }

  /** Sets the new password from a reset link and logs the player in. */
  resetPassword(token: string, newPassword: string): Observable<User> {
    return this.http
      .post<LoginResponse>(`${API_BASE}/auth/reset-password`, { token, newPassword })
      .pipe(
        tap((response) => this.store(response)),
        map((response) => response.user)
      );
  }

  /** Reloads the account, e.g. after the API said a step is required. */
  refresh(): Observable<User> {
    return this.http.get<User>(`${API_BASE}/auth/me`).pipe(tap((user) => this.updateUser(user)));
  }

  logout(): void {
    this.session.set(null);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // ignore
    }
  }

  /** Keeps the token; only a password change replaces it. */
  private updateUser(user: User): void {
    const token = this.token;
    if (token) {
      this.store({ token, user });
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

function loginFailure(error: unknown): LoginFailure {
  const status = error instanceof HttpErrorResponse ? error.status : 0;
  switch (status) {
    case 401:
      return 'credentials';
    case 403:
      return 'locked';
    case 429:
      return 'too-many';
    default:
      return 'unavailable';
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
