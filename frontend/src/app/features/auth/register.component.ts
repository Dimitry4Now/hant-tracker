import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BrandComponent } from '../../shared/brand.component';
import { ThemeToggleComponent } from '../../shared/theme-toggle.component';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, BrandComponent, ThemeToggleComponent],
  templateUrl: './register.component.html',
  styleUrl: './auth-card.scss'
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);

  readonly submitted = signal(false);

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

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    // Nothing to post to yet — the API has no account-request endpoint.
    this.submitted.set(true);
  }
}

function passwordsMatch(group: AbstractControl) {
  const password = group.get('password')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return password && confirm && password !== confirm ? { passwordMismatch: true } : null;
}
