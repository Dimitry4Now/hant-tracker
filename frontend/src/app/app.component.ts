import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from './core/theme.service';
import { ViewportService } from './core/viewport.service';
import { VersionFooterComponent } from './shared/version-footer.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, VersionFooterComponent],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    <router-outlet />
    @if (!viewport.isMobile()) {
      <app-version-footer />
    }
  `
})
export class AppComponent {
  // Instantiated here so the stored theme is applied on first paint.
  private readonly theme = inject(ThemeService);
  readonly viewport = inject(ViewportService);
}
