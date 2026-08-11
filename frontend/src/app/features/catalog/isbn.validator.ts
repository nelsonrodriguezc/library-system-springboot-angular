import { AbstractControl, ValidationErrors } from '@angular/forms';

/** Strips the separators people type, so 978-0-13-235088-4 and 9780132350884 are equal. */
export function normaliseIsbn(raw: string | null | undefined): string {
  return (raw ?? '').replace(/[^0-9Xx]/g, '').toUpperCase();
}

/**
 * Same check the server applies, run here so a typo is caught before the round trip.
 *
 * Deliberately duplicated rather than shared: the API is the authority, and the client
 * merely spares the reader a trip to find out what it already knows.
 */
export function isbnValidator(control: AbstractControl): ValidationErrors | null {
  const value = normaliseIsbn(control.value);
  if (!value) {
    return null; // emptiness is Validators.required's business
  }
  if (value.length === 10 ? isValidIsbn10(value) : value.length === 13 ? isValidIsbn13(value) : false) {
    return null;
  }
  return { isbn: true };
}

function isValidIsbn10(isbn: string): boolean {
  let sum = 0;
  for (let index = 0; index < 9; index++) {
    const digit = isbn.charCodeAt(index) - 48;
    if (digit < 0 || digit > 9) {
      return false;
    }
    sum += digit * (10 - index);
  }
  const last = isbn[9];
  const check = last === 'X' ? 10 : Number(last);
  return Number.isInteger(check) && (sum + check) % 11 === 0;
}

function isValidIsbn13(isbn: string): boolean {
  let sum = 0;
  for (let index = 0; index < 13; index++) {
    const digit = isbn.charCodeAt(index) - 48;
    if (digit < 0 || digit > 9) {
      return false;
    }
    sum += digit * (index % 2 === 0 ? 1 : 3);
  }
  return sum % 10 === 0;
}
