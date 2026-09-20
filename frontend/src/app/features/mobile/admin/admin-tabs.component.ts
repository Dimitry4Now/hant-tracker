import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { HantDataService } from '../../../core/hant-data.service';

/** "Admin" heading with the Requests / Users switch — one tab bar entry for both pages. */
@Component({
  selector: 'app-admin-tabs',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <h1 class="m-title">Admin</h1>
    <div class="m-segmented" role="tablist" aria-label="Admin sections">
      <a role="tab" routerLink="/admin/requests" routerLinkActive #req="routerLinkActive"
         [attr.aria-selected]="req.isActive" [replaceUrl]="true">
        Requests
        @if (pending() > 0) {
          <span class="count">{{ pending() }}</span>
        }
      </a>
      <a role="tab" routerLink="/admin/users" routerLinkActive #users="routerLinkActive"
         [attr.aria-selected]="users.isActive" [replaceUrl]="true">Users</a>
    </div>
  `,
  styles: [
    `
      :host {
        display: flex;
        flex-direction: column;
        gap: 16px;
      }

      a {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 6px;
        height: 36px;
        border-radius: 7px;
        color: var(--text-secondary);
        font-size: 14px;
        text-decoration: none;
      }

      a[aria-selected='true'] {
        background: var(--surface);
        color: var(--text);
        font-weight: 600;
        box-shadow: 0 1px 2px var(--shadow);
      }

      .count {
        min-width: 18px;
        height: 18px;
        padding: 0 5px;
        border-radius: 9px;
        background: var(--negative);
        color: #ffffff;
        font-size: 11px;
        font-weight: 600;
        display: inline-flex;
        align-items: center;
        justify-content: center;
      }
    `
  ]
})
export class AdminTabsComponent {
  readonly pending = signal(0);

  constructor() {
    inject(HantDataService)
      .getAccountRequests()
      .subscribe((requests) => this.pending.set(requests.length));
  }
}
