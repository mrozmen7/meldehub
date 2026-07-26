import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { CaseService } from './case.service';
import { CaseCreateRequest, CaseResponse } from '../models/case.model';

describe('CaseService', () => {
  let service: CaseService;
  let httpMock: HttpTestingController;

  const sampleCase: CaseResponse = {
    id: '3fa85f64-5717-4562-b3fc-2c963f66afa6',
    title: 'Kaldırımda büyük çukur',
    description: 'Bahnhofstrasse 12 önünde tehlikeli çukur var.',
    category: 'POTHOLE',
    status: 'NEW',
    location: 'Bahnhofstrasse 12, Zürich',
    reporterEmail: 'vatandas@example.ch',
    createdAt: '2026-07-25T10:00:00Z',
    updatedAt: '2026-07-25T10:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CaseService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('createCase POST /api/cases isteği göndermeli', () => {
    const request: CaseCreateRequest = {
      title: sampleCase.title,
      description: sampleCase.description,
      category: 'POTHOLE',
      location: sampleCase.location,
      reporterEmail: sampleCase.reporterEmail,
    };

    service.createCase(request).subscribe((res) => {
      expect(res).toEqual(sampleCase);
    });

    const req = httpMock.expectOne('/api/cases');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(sampleCase);
  });

  it('getCases GET /api/cases ile liste döndürmeli', () => {
    service.getCases().subscribe((cases) => {
      expect(cases.length).toBe(1);
      expect(cases[0].status).toBe('NEW');
    });

    const req = httpMock.expectOne('/api/cases');
    expect(req.request.method).toBe('GET');
    req.flush([sampleCase]);
  });

  it('getCase GET /api/cases/{id} ile tek vaka döndürmeli', () => {
    service.getCase(sampleCase.id).subscribe((res) => {
      expect(res.id).toBe(sampleCase.id);
    });

    const req = httpMock.expectOne(`/api/cases/${sampleCase.id}`);
    expect(req.request.method).toBe('GET');
    req.flush(sampleCase);
  });

  it('updateStatus PATCH /api/cases/{id}/status gövdesi {status} olmalı', () => {
    service.updateStatus(sampleCase.id, 'TRIAGED').subscribe((res) => {
      expect(res.status).toBe('TRIAGED');
    });

    const req = httpMock.expectOne(`/api/cases/${sampleCase.id}/status`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ status: 'TRIAGED' });
    req.flush({ ...sampleCase, status: 'TRIAGED' });
  });

  it('409 hatasında backend error mesajını iletmeli', () => {
    service.updateStatus(sampleCase.id, 'CLOSED').subscribe({
      next: () => {
        throw new Error('başarı beklenmiyordu');
      },
      error: (err) => {
        expect(err.status).toBe(409);
        expect(err.error.error).toBe('Geçersiz durum geçişi: NEW → CLOSED');
      },
    });

    const req = httpMock.expectOne(`/api/cases/${sampleCase.id}/status`);
    req.flush(
      { error: 'Geçersiz durum geçişi: NEW → CLOSED' },
      { status: 409, statusText: 'Conflict' }
    );
  });
});
