import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HantDataService } from '../../core/hant-data.service';
import { formatDate } from '../../core/game-stats';
import { User } from '../../core/models';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss'
})
export class UsersComponent {
  private readonly data = inject(HantDataService);
  private readonly fb = inject(FormBuilder);

  readonly users = signal<User[]>([]);
  readonly showForm = signal(false);
  readonly saving = signal(false);
  readonly formatDate = formatDate;

  readonly form = this.fb.nonNullable.group({
    fullName: ['', Validators.required],
    displayName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    role: ['USER' as User['role'], Validators.required]
  });

  constructor() {
    this.load();
  }

  openForm(): void {
    this.form.reset({ fullName: '', displayName: '', email: '', role: 'USER' });
    this.showForm.set(true);
  }

  cancel(): void {
    this.showForm.set(false);
  }

  create(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.data.createUser(this.form.getRawValue()).subscribe(() => {
      this.saving.set(false);
      this.showForm.set(false);
      this.load();
    });
  }

  toggleLock(user: User): void {
    const next = user.status === 'LOCKED' ? 'ACTIVE' : 'LOCKED';
    this.data.setUserStatus(user.id, next).subscribe(() => this.load());
  }

  rename(user: User): void {
    const name = prompt('New display name', user.displayName);
    if (name && name.trim()) {
      this.data.renameUser(user.id, name.trim()).subscribe(() => this.load());
    }
  }

  resetPassword(user: User): void {
    // Awaiting the API endpoint; the design only shows the action.
    alert(`A password reset would be sent to ${user.email}.`);
  }

  private load(): void {
    this.data.getUsers().subscribe((users) => this.users.set(users));
  }
}
