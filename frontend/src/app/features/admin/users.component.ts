import { Component, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { formatDate } from '../../core/game-stats';
import { User } from '../../core/models';
import { ViewportService } from '../../core/viewport.service';
import { UsersMobileComponent } from '../mobile/admin/users-mobile.component';
import { UsersStore } from './users.store';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, UsersMobileComponent],
  providers: [UsersStore],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss'
})
export class UsersComponent {
  private readonly store = inject(UsersStore);
  readonly viewport = inject(ViewportService);

  readonly users = this.store.users;
  readonly showForm = this.store.showForm;
  readonly saving = this.store.saving;
  readonly form = this.store.form;
  readonly formatDate = formatDate;

  openForm(): void {
    this.store.openForm();
  }

  cancel(): void {
    this.store.cancel();
  }

  create(): void {
    this.store.create();
  }

  toggleLock(user: User): void {
    this.store.toggleLock(user);
  }

  rename(user: User): void {
    this.store.rename(user);
  }

  isMe(user: User): boolean {
    return this.store.isMe(user);
  }

  changeEmail(user: User): void {
    this.store.changeEmail(user);
  }

  resetPassword(user: User): void {
    this.store.resetPassword(user);
  }
}
