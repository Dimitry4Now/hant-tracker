import { Component, input, output } from '@angular/core';
import { AccountRequest } from '../../../core/models';
import { dayMonth } from '../../../shared/mobile/dates';
import { initials } from '../../../shared/mobile/initials';
import { AdminTabsComponent } from './admin-tabs.component';

@Component({
  selector: 'app-requests-mobile',
  standalone: true,
  imports: [AdminTabsComponent],
  template: `
    <div class="m-page">
      <app-admin-tabs />
      <p class="m-hint">People who signed up and are waiting for access</p>

      @for (request of requests(); track request.id) {
        <article class="request">
          <div class="who">
            <span class="m-avatar lg">{{ initials(request.fullName) }}</span>
            <div class="grow">
              <div class="name">{{ request.fullName }}</div>
              <div class="m-hint">{{ request.email }}</div>
            </div>
            <span class="m-hint">{{ when(request.requestedOn) }}</span>
          </div>
          <div class="actions">
            <button type="button" class="m-btn-outline m-btn-danger" (click)="reject.emit(request.id)"
                    [attr.aria-label]="'Reject ' + request.fullName">Reject</button>
            <button type="button" class="m-btn" (click)="approve.emit(request.id)"
                    [attr.aria-label]="'Approve ' + request.fullName">Approve</button>
          </div>
        </article>
      } @empty {
        <p class="m-subtitle">Nothing waiting for review.</p>
      }

      @if (requests().length) {
        <p class="m-hint center">Approved people can log in straight away</p>
      }
    </div>
  `,
  styles: [
    `
      .request {
        background: var(--surface);
        border: 1px solid var(--border);
        border-radius: 12px;
        padding: 14px;
        display: flex;
        flex-direction: column;
        gap: 14px;
      }

      .who {
        display: flex;
        align-items: center;
        gap: 12px;
      }

      .grow {
        min-width: 0;
      }

      .name {
        font-size: 15px;
        font-weight: 600;
      }

      .actions {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 8px;
      }
    `
  ]
})
export class RequestsMobileComponent {
  readonly requests = input.required<AccountRequest[]>();
  readonly approve = output<number>();
  readonly reject = output<number>();

  readonly initials = initials;

  when(iso: string): string {
    return iso.slice(0, 10) === new Date().toISOString().slice(0, 10) ? 'Today' : dayMonth(iso.slice(0, 10));
  }
}
