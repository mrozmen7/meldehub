import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';

import { Login } from './login';
import { LoginResponse } from '../services/auth.service';

describe('Login (giriş ekranı)', () => {
  let httpMock: HttpTestingController;

  const loginResponse: LoginResponse = {
    token: 'jwt-token-123',
    username: 'operator',
    role: 'OPERATOR',
  };

  function createComponent() {
    const fixture = TestBed.createComponent(Login);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    return { fixture, component };
  }

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('başlangıçta form geçersiz olmalı', () => {
    const { component } = createComponent();
    expect(component.form.invalid).toBe(true);
  });

  it('boş form gönderildiğinde istek atılmamalı', () => {
    const { component } = createComponent();

    component.onSubmit();

    expect(component.form.controls.username.touched).toBe(true);
    httpMock.expectNone('/api/auth/login');
  });

  it('başarılı girişte returnUrl yoksa role göre varsayılana yönlendirmeli', () => {
    const { component } = createComponent();
    const router = TestBed.inject(Router);
    const navSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    component.form.setValue({ username: 'operator', password: 'meldehub123' });
    component.onSubmit();

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'operator', password: 'meldehub123' });
    req.flush(loginResponse);

    expect(localStorage.getItem('meldehub.session')).toContain('jwt-token-123');
    expect(navSpy).toHaveBeenCalledWith('/operator');
  });

  it('hatalı girişte backend hata mesajı kırmızı gösterilmeli', () => {
    const { component, fixture } = createComponent();

    component.form.setValue({ username: 'operator', password: 'yanlis' });
    component.onSubmit();

    const req = httpMock.expectOne('/api/auth/login');
    req.flush(
      { error: 'Kullanıcı adı veya şifre hatalı' },
      { status: 401, statusText: 'Unauthorized' }
    );
    fixture.detectChanges();

    expect(component.errorMessage()).toBe('Kullanıcı adı veya şifre hatalı');
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.alert-error')?.textContent).toContain(
      'Kullanıcı adı veya şifre hatalı'
    );
  });
});
