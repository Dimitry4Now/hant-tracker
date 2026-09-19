import { Component, computed, effect, inject, untracked } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { formatDate } from '../../../core/game-stats';
import { User } from '../../../core/models';
import { IconComponent } from '../../../shared/mobile/icon.component';
import { initials } from '../../../shared/mobile/initials';
import { queryState } from '../../../shared/mobile/query-state';
import { SheetComponent } from '../../../shared/mobile/sheet.component';
import { UsersStore } from '../../admin/users.store';
import { AdminTabsComponent } from './admin-tabs.component';

@Component({
  selector: 'app-users-mobile',
  standalone: true,
  imports: [NgTemplateOutlet, ReactiveFormsModule, RouterLink, IconComponent, SheetComponent, AdminTabsComponent],
  templateUrl: './users-mobile.component.html',
  styleUrl: './users-mobile.component.scss'
})
export class UsersMobileComponent {
  readonly store = inject(UsersStore);

  /** 'new' for the new-user sheet, or a user id for their actions. */
  readonly sheet = queryState('sheet');

  readonly selected = computed(() => {
    const id = Number(this.sheet.value());
    return this.store.users().find((user) => user.id === id) ?? null;
  });

  readonly initials = initials;
  readonly formatDate = formatDate;

  constructor() {
    effect(
      () => {
        const open = this.sheet.value() === 'new';
        untracked(() => {
          if (open && !this.store.showForm()) {
            this.store.openForm();
          } else if (!open && this.store.showForm()) {
            this.store.cancel();
          }
        });
      },
      { allowSignalWrites: true }
    );
  }

  open(user: User): void {
    this.sheet.open(String(user.id));
  }

  setRole(role: User['role']): void {
    this.store.form.controls.role.setValue(role);
  }

  create(): void {
    this.store.create(() => this.sheet.close());
  }
}
