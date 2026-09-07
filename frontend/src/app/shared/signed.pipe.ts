import { Pipe, PipeTransform } from '@angular/core';

/** Renders a number with an explicit sign, e.g. +80 / -50. */
@Pipe({ name: 'signed', standalone: true })
export class SignedPipe implements PipeTransform {
  transform(value: number | null | undefined, suffix = ''): string {
    if (value === null || value === undefined) {
      return '—';
    }
    const sign = value > 0 ? '+' : '';
    return `${sign}${value}${suffix ? ' ' + suffix : ''}`;
  }
}
