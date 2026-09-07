import { Injectable, computed, signal } from '@angular/core';
import { Observable, delay, of } from 'rxjs';
import { USERS } from './mock-data';
import { User } from './models';

const STORAGE_KEY = 'hant-user';

/**
 * Placeholder auth. Any password is accepted for a known email until the
 * API exposes a real login endpoint.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly currentUser = signal<User | null>(readStoredUser());

  readonly user = this.currentUser.asReadonly();
  readonly isLoggedIn = computed(() => this.currentUser() !== null);
  readonly isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');

  login(email: string, _password: string): Observable<User | null> {
    const user = USERS.find((u) => u.email.toLowerCase() === email.trim().toLowerCase()) ?? null;
    if (user) {
      this.currentUser.set(user);
      this.store(user);
    }
    return of(user).pipe(delay(200));
  }

  logout(): void {
    this.currentUser.set(null);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // ignore
    }
  }

  private store(user: User): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
    } catch {
      // ignore
    }
  }
}

function readStoredUser(): User | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as User) : null;
  } catch {
    return null;
  }
}
