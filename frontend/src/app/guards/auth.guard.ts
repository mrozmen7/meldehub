import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService, Role } from '../services/auth.service';

/**
 * Rol bazlı route guard fabrikası (CASE-201).
 *
 *   /report   → roleGuard('CITIZEN', 'OPERATOR') — giriş yapan herkes ihbar verebilir
 *   /operator → roleGuard('OPERATOR')            — panel sadece operatöre
 *
 * Giriş yoksa veya rol yetmezse /login'e yönlendirir; hedef returnUrl
 * query param'ında saklanır (login sonrası oraya dönülür).
 */
export function roleGuard(...roles: Role[]): CanActivateFn {
  return (_route, state) => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (auth.isLoggedIn() && auth.hasRole(...roles)) {
      return true;
    }
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  };
}
