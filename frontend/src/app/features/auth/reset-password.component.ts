import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ViewportService } from '../../core/viewport.service';
import { BrandComponent } from '../../shared/brand.component';
import { BackBarComponent } from '../../shared/mobile/back-bar.component';
import { ThemeToggleComponent } from '../../shared/theme-toggle.component';

/** Opened from the emailed link: sets a new password and logs the player in. */
@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, RouterLink, BrandComponent, ThemeToggleComponent, BackBarComponent],
  templateUrl: './reset-password.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './auth-card.scss'
})
export class ResetPasswordComponent {
  readonly viewport = inject(ViewportService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  readonly token = inject(ActivatedRoute).snapshot.queryParamMap.get('token') ?? '';
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  /** The link has run out or was used; only a new one helps. */
  readonly expired = signal(false);

  readonly form = this.fb.nonNullable.group(
    {
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    },
    { validators: passwordsMatch }
  );

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.error.set(null);
    this.saving.set(true);
    this.auth.resetPassword(this.token, this.form.getRawValue().newPassword).subscribe({
      next: (user) => void this.router.navigate([user.requiredStep ? '/setup' : '/dashboard']),
      error: (error: unknown) => {
        this.saving.set(false);
        const status = error instanceof HttpErrorResponse ? error.status : 0;
        if (status === 400 && this.form.getRawValue().newPassword.length >= 8) {
          // A short password is caught here already; a 400 is the link or the default password.
          this.error.set('This link has run out or was already used, or that password is not allowed.');
          this.expired.set(true);
        } else {
          this.error.set(
            status === 403
              ? 'This account is locked.'
              : status === 429
                ? 'Too many tries. Wait an hour and try again.'
                : 'Something went wrong. Try again.'
          );
        }
      }
    });
  }
}

function passwordsMatch(group: AbstractControl) {
  const password = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return password && confirm && password !== confirm ? { passwordMismatch: true } : null;
}
