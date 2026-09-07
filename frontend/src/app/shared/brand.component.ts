import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-brand',
  standalone: true,
  template: `
    <div class="brand">
      <svg [attr.width]="size" [attr.height]="size" viewBox="0 0 24 24" fill="none"
           stroke="var(--accent)" stroke-width="1.6" aria-hidden="true">
        <path d="M12 3c-3 3.5-7 6.4-7 10.2 0 2.5 2 4.3 4.3 4.3 1.2 0 2.2-.5 2.7-1.3-.2 1.6-1 2.9-2.4 3.8h4.8c-1.4-.9-2.2-2.2-2.4-3.8.5.8 1.5 1.3 2.7 1.3 2.3 0 4.3-1.8 4.3-4.3C19 9.4 15 6.5 12 3z" />
      </svg>
      <span class="name" [style.font-size.px]="size + 2">Hant Stats</span>
    </div>
  `,
  styles: [
    `
      .brand {
        display: flex;
        align-items: center;
        gap: 8px;
      }
      .name {
        font-family: var(--heading-font);
        font-weight: 700;
      }
    `
  ]
})
export class BrandComponent {
  @Input() size = 20;
}
