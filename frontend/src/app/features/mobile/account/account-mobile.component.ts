import { Component, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth.service';
import { formatDate } from '../../../core/game-stats';
import { ThemeService } from '../../../core/theme.service';
import { initials } from '../../../shared/mobile/initials';
import { AccountComponent } from '../../account/account.component';

/**
 * Account on phones. The forms and their saving live on the AccountComponent
 * that renders this, so it is injected rather than duplicated.
 */
@Component({
  selector: 'app-account-mobile',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './account-mobile.component.html',
  styleUrl: './account-mobile.component.scss'
})
export class AccountMobileComponent {
  readonly page = inject(AccountComponent);
  readonly auth = inject(AuthService);
  readonly theme = inject(ThemeService);
  private readonly router = inject(Router);

  readonly initials = initials;
  readonly formatDate = formatDate;

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/']);
  }
}
