import { Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { HantDataService } from '../core/hant-data.service';
import { ThemeService } from '../core/theme.service';
import { ViewportService } from '../core/viewport.service';
import { BrandComponent } from '../shared/brand.component';
import { IconComponent } from '../shared/mobile/icon.component';
import { initials } from '../shared/mobile/initials';
import { ThemeToggleComponent } from '../shared/theme-toggle.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    BrandComponent,
    ThemeToggleComponent,
    IconComponent
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss'
})
export class ShellComponent {
  private readonly router = inject(Router);
  private readonly data = inject(HantDataService);
  readonly auth = inject(AuthService);
  readonly theme = inject(ThemeService);
  readonly viewport = inject(ViewportService);

  readonly initials = initials;

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects)
    ),
    { initialValue: this.router.url }
  );

  /** The Admin tab covers both requests and users. */
  readonly adminActive = computed(() => /^\/admin\/(requests|users)/.test(this.url()));

  /** Shown as a badge on the Admin tab. */
  readonly pendingRequests = signal(0);

  constructor() {
    // Refreshed on every navigation so approving a request clears the badge.
    effect(() => {
      this.url();
      if (this.viewport.isMobile() && this.auth.isAdmin()) {
        this.data.getAccountRequests().subscribe((requests) => this.pendingRequests.set(requests.length));
      }
    });
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/']);
  }
}
