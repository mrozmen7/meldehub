import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';

import { roleGuard } from './auth.guard';

describe('roleGuard', () => {
  let router: Router;

  function runGuard(roles: Array<'CITIZEN' | 'OPERATOR'>, url: string) {
    const guard = roleGuard(...roles);
    const state = { url } as RouterStateSnapshot;
    return TestBed.runInInjectionContext(() => guard({} as ActivatedRouteSnapshot, state));
  }

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('giriş yoksa /login’e returnUrl ile yönlendirmeli', () => {
    const result = runGuard(['CITIZEN', 'OPERATOR'], '/report');

    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/login?returnUrl=%2Freport');
  });

  it('CITIZEN rolü operatör paneline girememeli', () => {
    localStorage.setItem(
      'meldehub.session',
      JSON.stringify({ token: 't', username: 'citizen', role: 'CITIZEN' })
    );

    const result = runGuard(['OPERATOR'], '/operator');

    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/login?returnUrl=%2Foperator');
  });

  it('OPERATOR rolü operatör paneline girebilmeli', () => {
    localStorage.setItem(
      'meldehub.session',
      JSON.stringify({ token: 't', username: 'operator', role: 'OPERATOR' })
    );

    expect(runGuard(['OPERATOR'], '/operator')).toBe(true);
  });

  it('CITIZEN rolü ihbar sayfasına girebilmeli', () => {
    localStorage.setItem(
      'meldehub.session',
      JSON.stringify({ token: 't', username: 'citizen', role: 'CITIZEN' })
    );

    expect(runGuard(['CITIZEN', 'OPERATOR'], '/report')).toBe(true);
  });
});
