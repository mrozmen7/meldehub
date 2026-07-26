import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('oturum varken /api isteğine Authorization: Bearer eklemeli', () => {
    localStorage.setItem(
      'meldehub.session',
      JSON.stringify({ token: 'jwt-token-123', username: 'operator', role: 'OPERATOR' })
    );

    http.get('/api/cases').subscribe();

    const req = httpMock.expectOne('/api/cases');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-token-123');
    req.flush([]);
  });

  it('oturum yokken isteği başlıksız göndermeli', () => {
    http.get('/api/cases').subscribe();

    const req = httpMock.expectOne('/api/cases');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('/api dışı isteklere token sızdırmamalı', () => {
    localStorage.setItem(
      'meldehub.session',
      JSON.stringify({ token: 'jwt-token-123', username: 'operator', role: 'OPERATOR' })
    );

    http.get('https://example.com/dis-servis').subscribe();

    const req = httpMock.expectOne('https://example.com/dis-servis');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });
});
