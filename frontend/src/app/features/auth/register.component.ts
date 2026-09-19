import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ViewportService } from '../../core/viewport.service';
import { BrandComponent } from '../../shared/brand.component';
import { BackBarComponent } from '../../shared/mobile/back-bar.component';
import { ThemeToggleComponent } from '../../shared/theme-toggle.component';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, BrandComponent, ThemeToggleComponent, BackBarComponent],
  templateUrl: './register.component.html',
  styleUrl: './auth-card.scss'
})
export class RegisterComponent {
  readonly viewport = inject(ViewportService);
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  readonly submitted = signal(false);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group(
    {
      fullName: ['', Validators.required],
      displayName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    },
    { validators: passwordsMatch }
  );

  /** Whether a field should show its message — only after the user has been there. */
  invalid(name: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.error.set(null);
    this.submitting.set(true);
    const { fullName, displayName, email, password } = this.form.getRawValue();
    this.auth.register({ fullName, displayName, email, password }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.submitted.set(true);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        const taken = error instanceof HttpErrorResponse && error.status === 409;
        this.error.set(
          taken ? 'That email already has an account or a pending request.' : 'Could not send the request.'
        );
      }
    });
  }
}

function passwordsMatch(group: AbstractControl) {
  const password = group.get('password')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return password && confirm && password !== confirm ? { passwordMismatch: true } : null;
}
