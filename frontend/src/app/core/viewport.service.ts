import { DestroyRef, Injectable, inject, signal } from '@angular/core';

/** Phones get the app layout. Keep in step with `$mobile-max` in styles/_mobile.scss. */
const MOBILE_QUERY = '(max-width: 767px)';

/**
 * Whether the screen is phone-sized. The desktop web and the mobile app
 * layout share routes, so pages switch on this rather than on the URL.
 */
@Injectable({ providedIn: 'root' })
export class ViewportService {
  readonly isMobile = signal(false);

  /**
   * Detail screens (a game's sheet, adding a round) bring their own header
   * and action bar, so the shell drops its app bar and tab bar while set.
   */
  readonly bare = signal(false);

  constructor() {
    const query = window.matchMedia?.(MOBILE_QUERY);
    if (!query) {
      return;
    }
    this.isMobile.set(query.matches);
    query.addEventListener('change', (event) => this.isMobile.set(event.matches));
  }
}

/** Hides the shell's mobile chrome for as long as the calling component lives. */
export function useBareChrome(): void {
  const viewport = inject(ViewportService);
  viewport.bare.set(true);
  inject(DestroyRef).onDestroy(() => viewport.bare.set(false));
}
