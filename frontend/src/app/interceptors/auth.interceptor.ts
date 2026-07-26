import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { AuthService } from '../services/auth.service';

/**
 * Auth interceptor (CASE-201) — her /api isteğine Bearer token ekler.
 *
 * Functional interceptor (Angular 22 tarzı). Token yoksa istek olduğu gibi
 * gider; backend 401 döner ve guard zaten /login'e yönlendirmiştir.
 * /api dışı isteklere (ör. varsa harici servis) token SIZDIRILMAZ.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).getToken();

  if (token && req.url.startsWith('/api')) {
    return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
  }
  return next(req);
};
