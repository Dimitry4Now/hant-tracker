import { Pipe, PipeTransform } from '@angular/core';

/** Denar is pegged to the euro; the site only ever shows euro for reference. */
export const DEN_PER_EUR = 61.5;

@Pipe({ name: 'denToEur', standalone: true })
export class DenToEurPipe implements PipeTransform {
  transform(amountDen: number | null | undefined): string {
    if (amountDen === null || amountDen === undefined) {
      return '—';
    }
    const eur = amountDen / DEN_PER_EUR;
    return `€${eur.toFixed(2)}`;
  }
}
