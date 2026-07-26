import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('oluşturulabilmeli', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('anonim kullanıcıya İhbar linki ve Giriş bağlantısı görünmeli', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.brand')?.textContent).toContain('MeldeHub');
    const links = Array.from(el.querySelectorAll('nav a')).map((a) => a.textContent?.trim());
    expect(links).toEqual(['İhbar Ver']);
    expect(el.querySelector('.login-link')?.textContent?.trim()).toBe('Giriş');
  });

  it('operatör giriş yapmışsa panel linki, kullanıcı adı ve çıkış butonu görünmeli', async () => {
    localStorage.setItem(
      'meldehub.session',
      JSON.stringify({ token: 't', username: 'operator', role: 'OPERATOR' })
    );
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    const links = Array.from(el.querySelectorAll('nav a')).map((a) => a.textContent?.trim());
    expect(links).toEqual(['İhbar Ver', 'Operatör Paneli']);
    expect(el.querySelector('.user-box .username')?.textContent).toContain('operator');
    expect(el.querySelector('.user-box button')?.textContent?.trim()).toBe('Çıkış');
  });
});
