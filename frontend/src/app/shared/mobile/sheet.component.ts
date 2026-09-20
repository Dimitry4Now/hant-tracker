import { Component, DestroyRef, inject, input, output } from '@angular/core';
import { IconComponent } from './icon.component';

/**
 * A bottom sheet over a dimmed page: handle, title, close button, scrolling
 * body and a footer slot (`[sheet-footer]`) that stays in view.
 */
@Component({
  selector: 'app-sheet',
  standalone: true,
  imports: [IconComponent],
  template: `
    <div class="m-sheet-backdrop" (click)="closed.emit()"></div>
    <section class="m-sheet" role="dialog" aria-modal="true" [attr.aria-label]="title()">
      <div class="m-sheet-handle"><span></span></div>
      <header class="m-sheet-head">
        <h2>{{ title() }}</h2>
        <button type="button" class="m-icon-btn" aria-label="Close" (click)="closed.emit()">
          <app-icon name="close" />
        </button>
      </header>
      <div class="m-sheet-body"><ng-content /></div>
      <footer class="m-sheet-foot"><ng-content select="[sheet-footer]" /></footer>
    </section>
  `
})
export class SheetComponent {
  readonly title = input.required<string>();
  readonly closed = output();

  constructor() {
    // The page behind should not scroll while the sheet is up.
    document.body.style.overflow = 'hidden';
    inject(DestroyRef).onDestroy(() => (document.body.style.overflow = ''));
  }
}
