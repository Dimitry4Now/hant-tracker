import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { ViewportService } from '../../core/viewport.service';
import { AccountMobileComponent } from '../mobile/account/account-mobile.component';

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [ReactiveFormsModule, AccountMobileComponent],
  templateUrl: './account.component.html',
  styleUrl: './account.component.scss'
})
export class AccountComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  readonly viewport = inject(ViewportService);

  readonly savingProfile = signal(false);
  readonly profileError = signal<string | null>(null);
  readonly profileSaved = signal(false);

  readonly savingPassword = signal(false);
  readonly passwordError = signal<string | null>(null);
  readonly passwordSaved = signal(false);

  readonly profile = this.fb.nonNullable.group({
    displayName: [this.auth.user()?.displayName ?? '', Validators.required],
    email: [this.auth.user()?.email ?? '', [Validators.required, Validators.email]],
    currentPassword: ['']
  });

  readonly password = this.fb.nonNullable.group(
    {
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    },
    { validators: passwordsMatch }
  );

  /** A new email needs the current password; a new display name does not. */
  emailChanged(): boolean {
    const current = this.auth.user()?.email ?? '';
    return this.profile.controls.email.value.trim().toLowerCase() !== current.toLowerCase();
  }

  saveProfile(): void {
    if (this.profile.invalid) {
      this.profile.markAllAsTouched();
      return;
    }
    const { displayName, email, currentPassword } = this.profile.getRawValue();
    if (this.emailChanged() && !currentPassword) {
      this.profileError.set('Enter your current password to change the email.');
      return;
    }

    this.profileError.set(null);
    this.profileSaved.set(false);
    this.savingProfile.set(true);
    this.auth
      .updateProfile({
        displayName: displayName.trim(),
        email: email.trim(),
        currentPassword: currentPassword || undefined
      })
      .subscribe({
        next: (user) => {
          this.savingProfile.set(false);
          this.profileSaved.set(true);
          this.profile.reset({ displayName: user.displayName, email: user.email, currentPassword: '' });
        },
        error: (error: unknown) => {
          this.savingProfile.set(false);
          this.profileError.set(describe(error, 'That email is already taken.'));
        }
      });
  }

  savePassword(): void {
    if (this.password.invalid) {
      this.password.markAllAsTouched();
      return;
    }
    const { currentPassword, newPassword } = this.password.getRawValue();

    this.passwordError.set(null);
    this.passwordSaved.set(false);
    this.savingPassword.set(true);
    this.auth.changePassword(currentPassword, newPassword).subscribe({
      next: () => {
        this.savingPassword.set(false);
        this.passwordSaved.set(true);
        this.password.reset();
      },
      error: (error: unknown) => {
        this.savingPassword.set(false);
        this.passwordError.set(describe(error));
      }
    });
  }
}

/** The API answers 400 for a wrong current password and 409 for a taken email. */
function describe(error: unknown, conflict = 'Could not save the changes.'): string {
  const status = error instanceof HttpErrorResponse ? error.status : 0;
  if (status === 400) {
    return 'Current password is wrong.';
  }
  if (status === 409) {
    return conflict;
  }
  return 'Could not save the changes.';
}

function passwordsMatch(group: AbstractControl) {
  const password = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return password && confirm && password !== confirm ? { passwordMismatch: true } : null;
}
