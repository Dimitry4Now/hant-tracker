import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { API_BASE } from './api.config';
import { authInterceptor } from './auth.interceptor';
import { AuthService, LoginResult } from './auth.service';
import { authGuard, setupGuard } from './guards';
import { RequiredStep, User } from './models';

function user(requiredStep: RequiredStep | null = null): User {
  return {
    id: 7,
    fullName: 'Ada Lovelace',
    displayName: 'Ada',
    email: 'ada@example.com',
    role: 'USER',
    status: 'ACTIVE',
    joinedOn: '2026-01-01',
    mustChangePassword: requiredStep === 'CHANGE_PASSWORD',
    emailVerified: requiredStep !== 'CONFIRM_EMAIL',
    requiredStep
  };
}

describe('auth', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()]
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => http.verify());

  function logIn(as: User): void {
    auth.login('ada@example.com', 'secret').subscribe();
    http.expectOne(`${API_BASE}/auth/login`).flush({ token: 'old-token', user: as });
  }

  function runGuard(guard: typeof authGuard): boolean | UrlTree {
    return TestBed.runInInjectionContext(() => guard({} as never, {} as never)) as boolean | UrlTree;
  }

  it('tells why a login failed', () => {
    const results: LoginResult[] = [];
    for (const status of [401, 403, 429, 500]) {
      auth.login('ada@example.com', 'x').subscribe((result) => results.push(result));
      http.expectOne(`${API_BASE}/auth/login`).flush({}, { status, statusText: 'x' });
    }
    expect(results).toEqual([
      { failure: 'credentials' },
      { failure: 'locked' },
      { failure: 'too-many' },
      { failure: 'unavailable' }
    ]);
    expect(auth.isLoggedIn()).toBe(false);
  });

  it('keeps a player with a step to take on the setup page', () => {
    logIn(user('CHANGE_PASSWORD'));
    expect(router.serializeUrl(runGuard(authGuard) as UrlTree)).toBe('/setup');
    expect(runGuard(setupGuard)).toBe(true);
  });

  it('lets a player with nothing to set up into the app, not the setup page', () => {
    logIn(user());
    expect(runGuard(authGuard)).toBe(true);
    expect(router.serializeUrl(runGuard(setupGuard) as UrlTree)).toBe('/dashboard');
  });

  it('stores the new token a password change answers with', () => {
    logIn(user('CHANGE_PASSWORD'));
    auth.changePassword('secret', 'a-better-one').subscribe();
    const request = http.expectOne(`${API_BASE}/account/password`);
    expect(request.request.headers.get('Authorization')).toBe('Bearer old-token');
    request.flush({ token: 'new-token', user: user() });

    expect(auth.token).toBe('new-token');
    expect(auth.requiredStep()).toBeNull();
  });

  it('sends the player to the setup page when the API says a step is required', () => {
    logIn(user());
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    TestBed.inject(HttpClient)
      .get(`${API_BASE}/games`)
      .subscribe({ error: () => undefined });
    http.expectOne(`${API_BASE}/games`).flush({ requiredStep: 'CONFIRM_EMAIL' }, { status: 428, statusText: 'x' });
    http.expectOne(`${API_BASE}/auth/me`).flush(user('CONFIRM_EMAIL'));

    expect(auth.isLoggedIn()).toBe(true);
    expect(auth.requiredStep()).toBe('CONFIRM_EMAIL');
    expect(navigate).toHaveBeenCalledWith(['/setup']);
  });

  it('drops the session when the token is refused', () => {
    logIn(user());
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    TestBed.inject(HttpClient)
      .get(`${API_BASE}/games`)
      .subscribe({ error: () => undefined });
    http.expectOne(`${API_BASE}/games`).flush({}, { status: 401, statusText: 'x' });

    expect(auth.isLoggedIn()).toBe(false);
  });
});
