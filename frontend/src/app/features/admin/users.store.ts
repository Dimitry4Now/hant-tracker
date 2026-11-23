import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { HantDataService } from '../../core/hant-data.service';
import { User } from '../../core/models';

/** A one-time password the admin has to pass on; shown once. */
export interface IssuedPassword {
  name: string;
  password: string;
}

/**
 * State behind the users page — the list, the new-user form and the per-user
 * actions — shared by the desktop page and its mobile layout.
 */
@Injectable()
export class UsersStore {
  private readonly data = inject(HantDataService);
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  readonly users = signal<User[]>([]);
  readonly showForm = signal(false);
  readonly saving = signal(false);
  readonly issued = signal<IssuedPassword | null>(null);
  readonly copied = signal(false);

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

  /** `done` runs once the user exists — mobile closes its sheet then. */
  create(done?: () => void): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.data.createUser(this.form.getRawValue()).subscribe(({ user, temporaryPassword }) => {
      this.saving.set(false);
      this.showForm.set(false);
      if (temporaryPassword) {
        this.issue(user.displayName, temporaryPassword);
      }
      this.load();
      done?.();
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

  /**
   * The admin's own row gets no actions: locking yourself locks you out, and
   * your name, email and password are edited on the account page.
   */
  isMe(user: User): boolean {
    return user.id === this.auth.user()?.id;
  }

  changeEmail(user: User): void {
    const email = prompt(`New email for ${user.displayName}`, user.email)?.trim();
    if (!email || email === user.email) {
      return;
    }
    this.data.changeUserEmail(user.id, email).subscribe({
      // Swap in the row the API sent back, so the grid never waits on a refetch.
      next: (updated) => this.users.update((users) => users.map((u) => (u.id === updated.id ? updated : u))),
      error: (error: unknown) => {
        const status = error instanceof HttpErrorResponse ? error.status : 0;
        alert(
          status === 409
            ? 'That email is already used by another account or a pending request.'
            : status === 400
              ? `"${email}" is not a valid email.`
              : 'Could not change the email.'
        );
      }
    });
  }

  /**
   * Gives the player a one-time password for the admin to pass on. It signs
   * them out everywhere, and they pick their own at their next login.
   */
  resetPassword(user: User, done?: () => void): void {
    if (!confirm(`Give ${user.displayName} a one-time password? Their current password stops working.`)) {
      return;
    }
    this.data.resetUserPassword(user.id).subscribe({
      next: (password) => {
        this.issue(user.displayName, password);
        this.load();
        done?.();
      },
      error: () => alert('Could not reset the password.')
    });
  }

  copyIssued(): void {
    const issued = this.issued();
    if (issued) {
      void navigator.clipboard?.writeText(issued.password).then(() => this.copied.set(true));
    }
  }

  dismissIssued(): void {
    this.issued.set(null);
  }

  private issue(name: string, password: string): void {
    this.copied.set(false);
    this.issued.set({ name, password });
  }

  private load(): void {
    this.data.getUsers().subscribe((users) => this.users.set(users));
  }
}
