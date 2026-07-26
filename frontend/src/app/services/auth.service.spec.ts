import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';

import { AuthService, LoginResponse } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const loginResponse: LoginResponse = {
    token: 'jwt-token-123',
    username: 'citizen',
    role: 'CITIZEN',
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('başlangıçta oturum olmamalı', () => {
    expect(service.isLoggedIn()).toBe(false);
    expect(service.currentUser()).toBeNull();
    expect(service.getToken()).toBeNull();
  });

  it('login POST /api/auth/login atmalı ve oturumu saklamalı', () => {
    service.login('citizen', 'meldehub123').subscribe((res) => {
      expect(res).toEqual(loginResponse);
    });

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'citizen', password: 'meldehub123' });
    req.flush(loginResponse);

    expect(service.isLoggedIn()).toBe(true);
    expect(service.getToken()).toBe('jwt-token-123');
    expect(service.currentUser()).toEqual({ username: 'citizen', role: 'CITIZEN' });
    expect(localStorage.getItem('meldehub.session')).toContain('jwt-token-123');
  });

  it('hasRole rol kontrolü yapmalı', () => {
    service.login('citizen', 'meldehub123').subscribe();
    httpMock.expectOne('/api/auth/login').flush(loginResponse);

    expect(service.hasRole('CITIZEN')).toBe(true);
    expect(service.hasRole('OPERATOR')).toBe(false);
    expect(service.hasRole('CITIZEN', 'OPERATOR')).toBe(true);
  });

  it('logout oturumu temizlemeli ve /login’e yönlendirmeli', () => {
    service.login('citizen', 'meldehub123').subscribe();
    httpMock.expectOne('/api/auth/login').flush(loginResponse);
    const router = TestBed.inject(Router);
    const navSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    service.logout();

    expect(service.isLoggedIn()).toBe(false);
    expect(service.getToken()).toBeNull();
    expect(localStorage.getItem('meldehub.session')).toBeNull();
    expect(navSpy).toHaveBeenCalledWith(['/login']);
  });
});

describe('AuthService — kalıcı oturum', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('localStorage’daki oturum sayfa yenilemede geri yüklenmeli', () => {
    localStorage.setItem(
      'meldehub.session',
      JSON.stringify({ token: 'jwt-token-123', username: 'operator', role: 'OPERATOR' })
    );
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    const service = TestBed.inject(AuthService);

    expect(service.isLoggedIn()).toBe(true);
    expect(service.currentUser()).toEqual({ username: 'operator', role: 'OPERATOR' });
    expect(service.getToken()).toBe('jwt-token-123');
  });

  it('bozuk localStorage kaydı temizlenip anonim başlanmalı', () => {
    localStorage.setItem('meldehub.session', '{bozuk-json');
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    const service = TestBed.inject(AuthService);

    expect(service.isLoggedIn()).toBe(false);
    expect(localStorage.getItem('meldehub.session')).toBeNull();
  });
});
