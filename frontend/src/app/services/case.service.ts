import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CaseCreateRequest, CaseResponse, CaseStatus, Page, StatusUpdateRequest } from '../models/case.model';

/**
 * MeldeHub vaka API istemcisi.
 * Tüm istekler /api altına gider; geliştirmede proxy.conf.json
 * /api → http://localhost:8080 yönlendirmesi yapar.
 */
@Injectable({ providedIn: 'root' })
export class CaseService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/cases';

  /** POST /api/cases — yeni vaka oluşturur (201 + CaseResponse). */
  createCase(request: CaseCreateRequest): Observable<CaseResponse> {
    return this.http.post<CaseResponse>(this.baseUrl, request);
  }

  /**
   * GET /api/cases — CASE-233: sayfalı liste (Page<CaseResponse>).
   * status verilirse backend filtreler; verilmezse tüm durumlar gelir.
   */
  getCases(page = 0, size = 20, status?: CaseStatus): Observable<Page<CaseResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<Page<CaseResponse>>(this.baseUrl, { params });
  }

  /** GET /api/cases/{id} — tek vaka getirir. */
  getCase(id: string): Observable<CaseResponse> {
    return this.http.get<CaseResponse>(`${this.baseUrl}/${id}`);
  }

  /** PATCH /api/cases/{id}/status — durum günceller; geçersiz geçişte 409 + {"error": "..."} döner. */
  updateStatus(id: string, status: CaseStatus): Observable<CaseResponse> {
    const body: StatusUpdateRequest = { status };
    return this.http.patch<CaseResponse>(`${this.baseUrl}/${id}/status`, body);
  }
}
