import { Component, inject, signal } from '@angular/core';
import { HantDataService } from '../../core/hant-data.service';
import { DashboardStats } from '../../core/models';
import { DenToEurPipe } from '../../shared/den-to-eur.pipe';
import { SignedPipe } from '../../shared/signed.pipe';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DenToEurPipe, SignedPipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  private readonly data = inject(HantDataService);

  readonly stats = signal<DashboardStats | null>(null);

  constructor() {
    this.data.getDashboard().subscribe((stats) => this.stats.set(stats));
  }
}
