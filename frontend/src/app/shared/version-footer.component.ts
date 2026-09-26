import { Component, ChangeDetectionStrategy } from '@angular/core';
import { appCommit, appVersion } from '../core/version';

/** The bar at the bottom of every web page. Phones have the tab bar there instead. */
@Component({
  selector: 'app-version-footer',
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `<footer>Version: {{ version }} ({{ commit }})</footer>`,
  styles: [
    `
      footer {
        height: var(--footer-height);
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 0 var(--gutter);
        border-top: 1px solid var(--border);
        background: var(--surface);
        color: var(--muted);
        font-size: 0.75rem;
        font-variant-numeric: tabular-nums;
      }
    `
  ]
})
export class VersionFooterComponent {
  readonly version = appVersion;
  readonly commit = appCommit;
}
