import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Reservation } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ReservationService {
  private readonly http = inject(HttpClient);

  create(bookId: number): Observable<Reservation> {
    return this.http.post<Reservation>('/api/reservations', { bookId });
  }

  mine(): Observable<Reservation[]> {
    return this.http.get<Reservation[]>('/api/reservations/mine');
  }

  cancel(reservationId: number): Observable<void> {
    return this.http.delete<void>(`/api/reservations/${reservationId}`);
  }
}
