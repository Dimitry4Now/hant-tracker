import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, LoginFailure } from '../../core/auth.service';
import { ViewportService } from '../../core/viewport.service';
import { BrandComponent } from '../../shared/brand.component';
import { BackBarComponent } from '../../shared/mobile/back-bar.component';
import { ThemeToggleComponent } from '../../shared/theme-toggle.component';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, BrandComponent, ThemeToggleComponent, BackBarComponent],
  templateUrl: './login.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './auth-card.scss'
})
export class LoginComponent {
  readonly viewport = inject(ViewportService);
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly error = signal<string | null>(null);
  readonly submitting = signal(false);
  /** "Forgot password?" only makes sense when the server can email a link. */
  readonly options = toSignal(this.auth.options$);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.error.set(null);
    this.submitting.set(true);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe((result) => {
      this.submitting.set(false);
      if ('user' in result) {
        void this.router.navigate([result.user.requiredStep ? '/setup' : '/dashboard']);
      } else {
        this.error.set(MESSAGES[result.failure]);
      }
    });
  }
}

const MESSAGES: Record<LoginFailure, string> = {
  credentials: 'Wrong email or password.',
  locked: 'This account is locked. Ask an admin.',
  'too-many': 'Too many attempts. Wait a few minutes and try again.',
  unavailable: 'Could not log in. Try again in a moment.'
};
