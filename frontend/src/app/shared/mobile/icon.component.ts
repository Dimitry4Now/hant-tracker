import { Component, input } from '@angular/core';

export type IconName =
  | 'dashboard'
  | 'analysis'
  | 'games'
  | 'admin'
  | 'account'
  | 'moon'
  | 'sun'
  | 'plus'
  | 'back'
  | 'close'
  | 'more'
  | 'chevron';

/** The line icons from the mobile artboards, drawn in the current text colour. */
@Component({
  selector: 'app-icon',
  standalone: true,
  host: { 'aria-hidden': 'true', style: 'display: inline-flex; flex-shrink: 0' },
  template: `
    <svg [attr.width]="size()" [attr.height]="size()" viewBox="0 0 24 24" fill="none"
         stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
      @switch (name()) {
        @case ('dashboard') {
          <rect x="4" y="4" width="7" height="7" rx="1.5" />
          <rect x="13" y="4" width="7" height="7" rx="1.5" />
          <rect x="4" y="13" width="7" height="7" rx="1.5" />
          <rect x="13" y="13" width="7" height="7" rx="1.5" />
        }
        @case ('analysis') {
          <path d="M5 20V11M12 20V5M19 20v-6" />
        }
        @case ('games') {
          <rect x="3.5" y="6" width="11" height="15" rx="2" transform="rotate(-8 9 13.5)" />
          <rect x="9.5" y="3" width="11" height="15" rx="2" transform="rotate(8 15 10.5)" />
        }
        @case ('admin') {
          <circle cx="9" cy="8" r="3.5" />
          <path d="M3 20c0-3.3 2.7-5.5 6-5.5s6 2.2 6 5.5" />
          <path d="M16 4.5a3.5 3.5 0 0 1 0 7M21 20c0-2.6-1.6-4.6-4-5.3" />
        }
        @case ('account') {
          <circle cx="12" cy="8" r="4" />
          <path d="M4 21c0-4 3.6-6.5 8-6.5s8 2.5 8 6.5" />
        }
        @case ('moon') {
          <path d="M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z" />
        }
        @case ('sun') {
          <circle cx="12" cy="12" r="4" />
          <path d="M12 2v3M12 19v3M4.2 4.2l2.1 2.1M17.7 17.7l2.1 2.1M2 12h3M19 12h3M4.2 19.8l2.1-2.1M17.7 6.3l2.1-2.1" />
        }
        @case ('plus') {
          <path d="M12 5v14M5 12h14" stroke-width="2" />
        }
        @case ('back') {
          <path d="M15 6l-6 6 6 6" stroke-width="2" />
        }
        @case ('close') {
          <path d="M6 6l12 12M18 6L6 18" stroke-width="2" />
        }
        @case ('more') {
          <circle cx="5" cy="12" r="1.7" fill="currentColor" stroke="none" />
          <circle cx="12" cy="12" r="1.7" fill="currentColor" stroke="none" />
          <circle cx="19" cy="12" r="1.7" fill="currentColor" stroke="none" />
        }
        @case ('chevron') {
          <path d="M9 6l6 6-6 6" stroke-width="2" />
        }
      }
    </svg>
  `
})
export class IconComponent {
  readonly name = input.required<IconName>();
  readonly size = input(22);
}
