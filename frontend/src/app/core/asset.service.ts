import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { Asset, AssetQuery, AssetRequest, PageResponse, ProblemDetail, StatusSummary } from './asset.model';

export const API_BASE = '/api/assets';

@Injectable({ providedIn: 'root' })
export class AssetService {
  private readonly http = inject(HttpClient);

  list(query: AssetQuery = {}): Observable<PageResponse<Asset>> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<PageResponse<Asset>>(API_BASE, { params }).pipe(catchError(toProblem));
  }

  get(id: number): Observable<Asset> {
    return this.http.get<Asset>(`${API_BASE}/${id}`).pipe(catchError(toProblem));
  }

  create(request: AssetRequest): Observable<Asset> {
    return this.http.post<Asset>(API_BASE, request).pipe(catchError(toProblem));
  }

  update(id: number, request: AssetRequest): Observable<Asset> {
    return this.http.put<Asset>(`${API_BASE}/${id}`, request).pipe(catchError(toProblem));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE}/${id}`).pipe(catchError(toProblem));
  }

  summary(): Observable<StatusSummary> {
    return this.http.get<StatusSummary>(`${API_BASE}/summary`).pipe(catchError(toProblem));
  }
}

/** Normalises any HTTP failure into a ProblemDetail so components handle one shape. */
export function toProblem(error: HttpErrorResponse): Observable<never> {
  const body = error.error as ProblemDetail | null;
  const problem: ProblemDetail =
    body && typeof body === 'object' && ('title' in body || 'detail' in body)
      ? body
      : {
          status: error.status,
          title: error.status === 0 ? 'Network error' : 'Request failed',
          detail: error.status === 0 ? 'The API could not be reached.' : error.message,
        };
  return throwError(() => problem);
}
