import { Component, inject, signal } from '@angular/core';
import { HantDataService } from '../../core/hant-data.service';
import { formatDate } from '../../core/game-stats';
import { AccountRequest } from '../../core/models';
import { ViewportService } from '../../core/viewport.service';
import { RequestsMobileComponent } from '../mobile/admin/requests-mobile.component';

@Component({
  selector: 'app-requests',
  standalone: true,
  imports: [RequestsMobileComponent],
  templateUrl: './requests.component.html',
  styleUrl: './requests.component.scss'
})
export class RequestsComponent {
  private readonly data = inject(HantDataService);
  readonly viewport = inject(ViewportService);

  readonly requests = signal<AccountRequest[]>([]);
  readonly formatDate = formatDate;

  constructor() {
    this.load();
  }

  approve(id: number): void {
    this.data.approveRequest(id).subscribe(() => this.load());
  }

  reject(id: number): void {
    this.data.rejectRequest(id).subscribe(() => this.load());
  }

  private load(): void {
    this.data.getAccountRequests().subscribe((requests) => this.requests.set(requests));
  }
}
