import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { SignedPipe } from '../../../shared/signed.pipe';
import { GameDetailStore } from '../game-detail.store';
import { SheetImportStore } from './sheet-import.store';

/** Desktop panel on an empty game's page: upload a photo of the paper sheet and check what was read. */
@Component({
  selector: 'app-sheet-import',
  imports: [SignedPipe],
  providers: [SheetImportStore],
  templateUrl: './sheet-import.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './sheet-import.component.scss'
})
export class SheetImportComponent {
  readonly store = inject(SheetImportStore);
  readonly game = inject(GameDetailStore);

  pick(input: HTMLInputElement): void {
    const file = input.files?.[0];
    input.value = '';
    if (file) {
      this.store.read(file);
    }
  }

  columns(): string {
    return `3.5rem repeat(${this.game.players().length}, minmax(0, 1fr)) 2rem`;
  }
}
