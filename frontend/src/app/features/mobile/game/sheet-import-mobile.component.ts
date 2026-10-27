import { Component, inject, output, ChangeDetectionStrategy } from '@angular/core';
import { formatDate } from '../../../core/game-stats';
import { IconComponent } from '../../../shared/mobile/icon.component';
import { SignedPipe } from '../../../shared/signed.pipe';
import { GameDetailStore } from '../../admin/game-detail.store';
import { SheetImportStore } from '../../admin/sheet-import/sheet-import.store';

/** Full-screen sheet import on phones: take or pick a photo of the paper sheet, check it, save. */
@Component({
  selector: 'app-sheet-import-mobile',
  imports: [IconComponent, SignedPipe],
  providers: [SheetImportStore],
  templateUrl: './sheet-import-mobile.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './sheet-import-mobile.component.scss'
})
export class SheetImportMobileComponent {
  readonly store = inject(SheetImportStore);
  readonly game = inject(GameDetailStore);
  readonly closed = output();

  readonly formatDate = formatDate;

  pick(input: HTMLInputElement): void {
    const file = input.files?.[0];
    input.value = '';
    if (file) {
      this.store.read(file);
    }
  }

  save(): void {
    this.store.save(() => this.closed.emit());
  }
}
