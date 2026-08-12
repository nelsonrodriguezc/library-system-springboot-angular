import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AdminStats, PageResponse, UserSummary } from '../models/api.models';

/** Endpoints under /api/admin. The server restricts every one of them to the ADMIN role. */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  stats(): Observable<AdminStats> {
    return this.http.get<AdminStats>('/api/admin/stats');
  }

  /** @param blocked undefined lists everyone, true only blocked, false only active. */
  users(search: string, blocked: boolean | undefined, page = 0, size = 20): Observable<PageResponse<UserSummary>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search.trim()) {
      params = params.set('search', search.trim());
    }
    if (blocked !== undefined) {
      params = params.set('blocked', blocked);
    }
    return this.http.get<PageResponse<UserSummary>>('/api/admin/users', { params });
  }

  unblock(userId: number): Observable<UserSummary> {
    return this.http.put<UserSummary>(`/api/admin/users/${userId}/unblock`, {});
  }

  sendDueSoonReminders(): Observable<{ sent: number }> {
    return this.http.post<{ sent: number }>('/api/admin/notifications/due-soon-reminders', {});
  }

  sendOverdueNotices(): Observable<{ sent: number }> {
    return this.http.post<{ sent: number }>('/api/admin/notifications/overdue-notices', {});
  }
}
