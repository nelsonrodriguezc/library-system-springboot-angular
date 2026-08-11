import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Book,
  BookPreview,
  BookRecommendation,
  BookStatus,
  CreateBookRequest,
  PageResponse,
} from '../models/api.models';

export interface CatalogueQuery {
  search?: string;
  status?: BookStatus | '';
  subject?: string;
  sort?: string;
  page?: number;
  size?: number;
}

/**
 * Everything the catalogue endpoints offer.
 *
 * The bearer token is not touched here: the HTTP interceptor attaches it to every call,
 * which is what keeps that concern out of a dozen services.
 */
@Injectable({ providedIn: 'root' })
export class BookService {
  private readonly http = inject(HttpClient);

  search(query: CatalogueQuery): Observable<PageResponse<Book>> {
    let params = new HttpParams()
      .set('page', query.page ?? 0)
      .set('size', query.size ?? 12)
      .set('sort', query.sort || 'createdAt,desc');

    if (query.search?.trim()) {
      params = params.set('search', query.search.trim());
    }
    if (query.status) {
      params = params.set('status', query.status);
    }
    if (query.subject) {
      params = params.set('subject', query.subject);
    }
    return this.http.get<PageResponse<Book>>('/api/books', { params });
  }

  byId(id: number): Observable<Book> {
    return this.http.get<Book>(`/api/books/${id}`);
  }

  subjects(): Observable<string[]> {
    return this.http.get<string[]>('/api/books/subjects');
  }

  /** Preview from the external catalogue. Nothing is stored by this call. */
  lookup(isbn: string): Observable<BookPreview> {
    return this.http.get<BookPreview>(`/api/books/lookup/${encodeURIComponent(isbn)}`);
  }

  create(request: CreateBookRequest): Observable<Book> {
    return this.http.post<Book>('/api/books', request);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`/api/books/${id}`);
  }

  recommendations(limit = 3): Observable<BookRecommendation[]> {
    return this.http.get<BookRecommendation[]>('/api/books/recommendations', {
      params: new HttpParams().set('limit', limit),
    });
  }
}
