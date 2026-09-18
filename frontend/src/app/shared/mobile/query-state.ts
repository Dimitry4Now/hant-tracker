import { Location } from '@angular/common';
import { DestroyRef, Signal, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { map } from 'rxjs';

export interface QueryState {
  readonly value: Signal<string | null>;
  /** Pushes a history entry, so the phone's back button undoes it. */
  open(value: string): void;
  /** Switches in place, without a history entry — for tabs. */
  replace(value: string | null): void;
  close(): void;
}

/**
 * A piece of mobile screen state — an open sheet, the selected tab — kept in
 * a query param, so it survives a reload and the back button closes sheets.
 * Call from a component's injection context.
 */
export function queryState(key: string): QueryState {
  const route = inject(ActivatedRoute);
  const router = inject(Router);
  const location = inject(Location);

  const value = toSignal(route.queryParamMap.pipe(map((params) => params.get(key))), {
    initialValue: route.snapshot.queryParamMap.get(key)
  });

  // Whether the current value came from open() in this page, so close() can
  // step back through history rather than leave the entry behind.
  let pushed = false;
  const subscription = route.queryParamMap.subscribe((params) => {
    if (params.get(key) === null) {
      pushed = false;
    }
  });
  inject(DestroyRef).onDestroy(() => subscription.unsubscribe());

  const navigate = (next: string | null, replaceUrl: boolean) =>
    void router.navigate([], {
      relativeTo: route,
      queryParams: { [key]: next },
      queryParamsHandling: 'merge',
      replaceUrl
    });

  return {
    value,
    open(next) {
      navigate(next, pushed);
      pushed = true;
    },
    replace(next) {
      navigate(next, true);
    },
    close() {
      if (pushed) {
        pushed = false;
        location.back();
      } else {
        navigate(null, true);
      }
    }
  };
}
