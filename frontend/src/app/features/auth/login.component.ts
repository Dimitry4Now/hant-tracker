import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { BrandComponent } from '../../shared/brand.component';
import { ThemeToggleComponent } from '../../shared/theme-toggle.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, BrandComponent, ThemeToggleComponent],
  templateUrl: './login.component.html',
  styleUrl: './auth-card.scss'
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly error = signal<string | null>(null);
  readonly submitting = signal(false);

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
    this.auth.login(email, password).subscribe((user) => {
      this.submitting.set(false);
      if (user) {
        void this.router.navigate(['/dashboard']);
      } else {
        this.error.set('No account matches that email.');
      }
    });
  }
}
