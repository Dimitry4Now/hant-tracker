import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { User } from '../../core/models';
import { ViewportService } from '../../core/viewport.service';
import { BrandComponent } from '../../shared/brand.component';
import { EmailConfirmComponent } from '../../shared/email-confirm.component';
import { BackBarComponent } from '../../shared/mobile/back-bar.component';
import { ThemeToggleComponent } from '../../shared/theme-toggle.component';

/**
 * What a player must do before the app opens: replace a password nobody
 * should keep (the old default, or one an admin set), then confirm a real
 * email when the server can send one. The API refuses everything else until
 * then (428).
 */
@Component({
  selector: 'app-setup',
  imports: [ReactiveFormsModule, BrandComponent, ThemeToggleComponent, BackBarComponent, EmailConfirmComponent],
  templateUrl: './setup.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './auth-card.scss'
})
export class SetupComponent {
  readonly viewport = inject(ViewportService);
  readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  readonly password = this.fb.nonNullable.group(
    {
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    },
    { validators: passwordsMatch }
  );

  savePassword(): void {
    if (this.password.invalid) {
      this.password.markAllAsTouched();
      return;
    }
    const { currentPassword, newPassword } = this.password.getRawValue();
    this.error.set(null);
    this.saving.set(true);
    this.auth.changePassword(currentPassword, newPassword).subscribe({
      next: (user) => {
        this.saving.set(false);
        this.next(user);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        const status = error instanceof HttpErrorResponse ? error.status : 0;
        this.error.set(
          status === 400
            ? 'Check the current password, and pick a new one that is neither the old nor the default one.'
            : 'Could not change the password. Try again.'
        );
      }
    });
  }

  /** On to the next step, or into the app once there is none. */
  next(user: User): void {
    if (!user.requiredStep) {
      void this.router.navigate(['/dashboard']);
    }
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}

function passwordsMatch(group: AbstractControl) {
  const password = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return password && confirm && password !== confirm ? { passwordMismatch: true } : null;
}
