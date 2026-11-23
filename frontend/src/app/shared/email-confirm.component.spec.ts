import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE } from '../core/api.config';
import { User } from '../core/models';
import { EmailConfirmComponent } from './email-confirm.component';

describe('EmailConfirmComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [EmailConfirmComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends a code, then confirms the email with it', () => {
    const fixture = TestBed.createComponent(EmailConfirmComponent);
    fixture.componentRef.setInput('initialEmail', 'made-up@example.com');
    fixture.detectChanges();
    const form = fixture.componentInstance;
    const confirmed: User[] = [];
    form.confirmed.subscribe((user) => confirmed.push(user));

    form.emailForm.setValue({ email: 'real@example.com' });
    form.send();
    const send = http.expectOne(`${API_BASE}/account/email`);
    expect(send.request.body).toEqual({ email: 'real@example.com' });
    send.flush(null, { status: 202, statusText: 'Accepted' });
    expect(form.sentTo()).toBe('real@example.com');

    form.codeForm.setValue({ code: '12345' });
    form.confirm();
    expect(form.error()).toContain('six digits');

    form.codeForm.setValue({ code: '123456' });
    form.confirm();
    http.expectOne(`${API_BASE}/account/email/confirm`).flush({ email: 'real@example.com' } as User);
    expect(confirmed.map((user) => user.email)).toEqual(['real@example.com']);
  });

  it('says when the email belongs to someone else', () => {
    const fixture = TestBed.createComponent(EmailConfirmComponent);
    fixture.detectChanges();
    const form = fixture.componentInstance;

    form.emailForm.setValue({ email: 'taken@example.com' });
    form.send();
    http.expectOne(`${API_BASE}/account/email`).flush({}, { status: 409, statusText: 'Conflict' });

    expect(form.sentTo()).toBeNull();
    expect(form.error()).toContain('another account');
  });
});
