import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { API_BASE } from '../../core/api.config';
import { Game, User, UserRole } from '../../core/models';
import { GameDetailComponent } from './game-detail.component';

const GAME: Game = {
  id: 5,
  playedOn: '2026-08-18',
  gameOfDay: 1,
  gamesThatDay: 1,
  inProgress: false,
  majstorska: true,
  note: null,
  playerIds: [1, 2],
  rounds: [
    {
      id: 50,
      number: 1,
      dealerId: 1,
      majstorska: false,
      hant: false,
      comment: 'a tight one',
      entries: [
        { playerId: 1, outcome: 'WINNER', cardValue: null, points: -20 },
        { playerId: 2, outcome: 'OPENED', cardValue: 12, points: 12 }
      ]
    }
  ],
  money: [
    { playerId: 1, amountDen: 300 },
    { playerId: 2, amountDen: -300 }
  ]
};

function user(id: number, displayName: string, role: UserRole = 'USER'): User {
  return {
    id,
    displayName,
    role,
    fullName: '',
    email: '',
    status: 'ACTIVE',
    joinedOn: '2025-01-01',
    mustChangePassword: false,
    emailVerified: true,
    requiredStep: null
  };
}

/** Renders the game page signed in with the given role. */
function render(role: UserRole): HTMLElement {
  localStorage.setItem('hant-auth', JSON.stringify({ token: 't', user: user(9, 'Me', role) }));
  TestBed.configureTestingModule({
    imports: [GameDetailComponent],
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '5' }) } } }
    ]
  });
  const fixture = TestBed.createComponent(GameDetailComponent);
  const http = TestBed.inject(HttpTestingController);
  fixture.detectChanges();
  http.expectOne(`${API_BASE}/games/5`).flush(GAME);
  http.expectOne(`${API_BASE}/users`).flush([user(1, 'Ana'), user(2, 'Bob')]);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('GameDetailComponent', () => {
  afterEach(() => localStorage.clear());

  it('shows players every round, read-only', () => {
    const page = render('USER');
    const text = page.textContent ?? '';

    expect(text).toContain('Ana');
    expect(text).toContain('a tight one');
    expect(text).toContain('300');
    for (const control of ['Edit game', 'Delete game', 'Save amounts', 'Save round', 'Reopen game']) {
      expect(text).not.toContain(control);
    }
    expect(page.querySelectorAll('input, select, textarea').length).toBe(0);
    expect(page.querySelector('.crumbs a')?.getAttribute('href')).toBe('/analysis');
  });

  it('gives admins the editing controls', () => {
    const page = render('ADMIN');
    const text = page.textContent ?? '';

    for (const control of ['Edit game', 'Delete game', 'Save amounts', 'Save round']) {
      expect(text).toContain(control);
    }
    expect(page.querySelector('.crumbs a')?.getAttribute('href')).toBe('/admin/games');
  });
});
