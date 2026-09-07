import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { BrandComponent } from '../shared/brand.component';
import { ThemeToggleComponent } from '../shared/theme-toggle.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, BrandComponent, ThemeToggleComponent],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss'
})
export class ShellComponent {
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/']);
  }
}
