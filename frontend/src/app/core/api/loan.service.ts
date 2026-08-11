import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Loan, PageResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class LoanService {
  private readonly http = inject(HttpClient);

  /** @param borrowerEmail only an ADMIN may lend on behalf of another account. */
  create(bookId: number, borrowerEmail?: string): Observable<Loan> {
    return this.http.post<Loan>('/api/loans', { bookId, borrowerEmail });
  }

  mine(page = 0, size = 50): Observable<PageResponse<Loan>> {
    return this.http.get<PageResponse<Loan>>('/api/loans/mine', {
      params: new HttpParams().set('page', page).set('size', size),
    });
  }

  return(loanId: number): Observable<Loan> {
    return this.http.put<Loan>(`/api/loans/${loanId}/return`, {});
  }
}
