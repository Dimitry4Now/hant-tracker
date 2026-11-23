import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ViewportService } from '../../core/viewport.service';
import { BrandComponent } from '../../shared/brand.component';
import { BackBarComponent } from '../../shared/mobile/back-bar.component';
import { ThemeToggleComponent } from '../../shared/theme-toggle.component';

/** Asks for a reset link. The answer is the same whether the email has an account or not. */
@Component({
  selector: 'app-forgot-password',
  imports: [ReactiveFormsModule, RouterLink, BrandComponent, ThemeToggleComponent, BackBarComponent],
  templateUrl: './forgot-password.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './auth-card.scss'
})
export class ForgotPasswordComponent {
  readonly viewport = inject(ViewportService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly sent = signal(false);
  readonly sending = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({ email: ['', [Validators.required, Validators.email]] });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.error.set(null);
    this.sending.set(true);
    this.auth.forgotPassword(this.form.getRawValue().email).subscribe({
      next: () => {
        this.sending.set(false);
        this.sent.set(true);
      },
      error: (error: unknown) => {
        this.sending.set(false);
        const status = error instanceof HttpErrorResponse ? error.status : 0;
        this.error.set(
          status === 429
            ? 'Too many tries. Wait an hour and try again.'
            : status === 503
              ? "Resetting by email isn't available. Ask an admin to reset your password."
              : 'Something went wrong. Try again.'
        );
      }
    });
  }
}
