import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, output, signal, ChangeDetectionStrategy, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../core/auth.service';
import { User } from '../core/models';

/**
 * Confirms an email with a code: the address first, then the six digits the
 * API emailed to it. The account's email changes only once the code is in.
 * Used by the setup page and the account page, desktop and phone.
 */
@Component({
  selector: 'app-email-confirm',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    @if (sentTo(); as address) {
      <form [formGroup]="codeForm" (ngSubmit)="confirm()">
        <p [class]="mobile() ? 'm-hint' : 'hint'">
          We emailed a code to <strong>{{ address }}</strong>. It works for 15 minutes.
        </p>
        <div [class]="mobile() ? 'm-field' : 'field'">
          <label [for]="id('code')">Code</label>
          <input
            [id]="id('code')"
            [class.m-input]="mobile()"
            type="text"
            inputmode="numeric"
            autocomplete="one-time-code"
            maxlength="6"
            formControlName="code"
            placeholder="123456"
          />
        </div>
        @if (error(); as message) {
          <span class="error">{{ message }}</span>
        }
        <div class="actions">
          <button type="submit" [class]="mobile() ? 'm-btn block' : 'btn'" [disabled]="busy()">
            {{ busy() ? 'Checking…' : 'Confirm email' }}
          </button>
          <button type="button" class="link" (click)="back()">Use another email</button>
        </div>
      </form>
    } @else {
      <form [formGroup]="emailForm" (ngSubmit)="send()">
        <div [class]="mobile() ? 'm-field' : 'field'">
          <label [for]="id('email')">{{ label() }}</label>
          <input
            [id]="id('email')"
            [class.m-input]="mobile()"
            type="email"
            autocomplete="email"
            formControlName="email"
            placeholder="you@example.com"
          />
        </div>
        @if (emailForm.invalid && emailForm.touched) {
          <span class="error">Enter a valid email.</span>
        }
        @if (error(); as message) {
          <span class="error">{{ message }}</span>
        }
        <div class="actions">
          <button type="submit" [class]="mobile() ? 'm-btn block' : 'btn'" [disabled]="busy()">
            {{ busy() ? 'Sending…' : 'Send code' }}
          </button>
        </div>
      </form>
    }
  `,
  styles: `
    form {
      display: flex;
      flex-direction: column;
      gap: 14px;
    }
    .actions {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .hint {
      font-size: 0.8125rem;
      color: var(--muted);
    }
    .link {
      align-self: center;
      background: none;
      border: none;
      padding: 4px;
      color: var(--accent);
      font: inherit;
      font-size: 0.8125rem;
      text-decoration: underline;
      cursor: pointer;
    }
  `
})
export class EmailConfirmComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly mobile = input(false);
  readonly initialEmail = input('');
  readonly label = input('Email');
  /** Keeps ids unique when the form appears twice on a page. */
  readonly idPrefix = input('email-confirm');
  readonly confirmed = output<User>();

  readonly sentTo = signal<string | null>(null);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);

  readonly emailForm = this.fb.nonNullable.group({ email: ['', [Validators.required, Validators.email]] });
  readonly codeForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^\s*\d{6}\s*$/)]]
  });

  ngOnInit(): void {
    this.emailForm.setValue({ email: this.initialEmail() });
  }

  id(name: string): string {
    return `${this.idPrefix()}-${name}`;
  }

  send(): void {
    if (this.emailForm.invalid) {
      this.emailForm.markAllAsTouched();
      return;
    }
    const email = this.emailForm.getRawValue().email.trim();
    this.start();
    this.auth.sendEmailCode(email).subscribe({
      next: () => {
        this.busy.set(false);
        this.codeForm.reset();
        this.sentTo.set(email);
      },
      error: (error: unknown) => this.fail(error, 'send')
    });
  }

  confirm(): void {
    if (this.codeForm.invalid) {
      this.error.set('Type the six digits from the email.');
      return;
    }
    this.start();
    this.auth.confirmEmailCode(this.codeForm.getRawValue().code).subscribe({
      next: (user) => {
        this.busy.set(false);
        this.sentTo.set(null);
        this.confirmed.emit(user);
      },
      error: (error: unknown) => this.fail(error, 'confirm')
    });
  }

  back(): void {
    this.error.set(null);
    this.sentTo.set(null);
  }

  private start(): void {
    this.error.set(null);
    this.busy.set(true);
  }

  private fail(error: unknown, step: 'send' | 'confirm'): void {
    this.busy.set(false);
    const status = error instanceof HttpErrorResponse ? error.status : 0;
    this.error.set(
      status === 409
        ? 'That email belongs to another account.'
        : status === 429
          ? 'Too many codes asked for. Try again in an hour.'
          : status === 503
            ? 'Could not send the email. Try again later.'
            : status === 400 && step === 'confirm'
              ? 'That code is wrong or has run out. Check it, or ask for a new one.'
              : 'Something went wrong. Try again.'
    );
  }
}
