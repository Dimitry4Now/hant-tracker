import { Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ThemeService } from '../../core/theme.service';
import { IconComponent } from './icon.component';

/** Mobile header for the signed-out pages: back arrow, title, theme switch. */
@Component({
  selector: 'app-back-bar',
  standalone: true,
  imports: [RouterLink, IconComponent],
  template: `
    <header class="m-detail-bar">
      <a class="m-icon-btn" [routerLink]="back()" aria-label="Back">
        <app-icon name="back" />
      </a>
      <div class="grow m-detail-title">{{ title() }}</div>
      <button type="button" class="m-icon-btn" (click)="theme.toggle()"
              [attr.aria-label]="theme.theme() === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'">
        <app-icon [name]="theme.theme() === 'dark' ? 'sun' : 'moon'" [size]="20" />
      </button>
    </header>
  `
})
export class BackBarComponent {
  readonly title = input.required<string>();
  readonly back = input('/');
  readonly theme = inject(ThemeService);
}
