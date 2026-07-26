import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

/** Backend Role enum'u ile birebir — ch.meldehub.domain.Role */
export type Role = 'CITIZEN' | 'OPERATOR';

/** POST /api/auth/login cevabı — backend: LoginResponse record */
export interface LoginResponse {
  token: string;
  username: string;
  role: Role;
}

/** localStorage'da saklanan oturum kaydı */
interface StoredSession {
  token: string;
  username: string;
  role: Role;
}

/** Giriş yapmış kullanıcının görünümü */
export interface CurrentUser {
  username: string;
  role: Role;
}

const STORAGE_KEY = 'meldehub.session';

/**
 * Kimlik doğrulama servisi (CASE-201).
 *
 * Token localStorage'da tutulur: sayfa yenilemede oturum korunur.
 * JWT'nin imzasını backend doğrular; frontend token'ı okumaz/çözümlemez,
 * kullanıcı bilgisini login cevabından alır ve olduğu gibi saklar.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  /** Giriş yapmış kullanıcı; yoksa null. */
  readonly currentUser = signal<CurrentUser | null>(this.loadSession());

  /** POST /api/auth/login — başarıda oturumu saklar. */
  login(username: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>('/api/auth/login', { username, password })
      .pipe(tap((res) => this.storeSession(res)));
  }

  /** Oturumu kapatır ve giriş ekranına yönlendirir. */
  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.currentUser.set(null);
    void this.router.navigate(['/login']);
  }

  /** HTTP interceptor'ın kullandığı Bearer token; yoksa null. */
  getToken(): string | null {
    return this.readStoredSession()?.token ?? null;
  }

  isLoggedIn(): boolean {
    return this.currentUser() !== null;
  }

  /** Kullanıcı verilen rollerden birine sahip mi? */
  hasRole(...roles: Role[]): boolean {
    const user = this.currentUser();
    return user !== null && roles.includes(user.role);
  }

  private storeSession(res: LoginResponse): void {
    const session: StoredSession = { token: res.token, username: res.username, role: res.role };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    this.currentUser.set({ username: res.username, role: res.role });
  }

  private loadSession(): CurrentUser | null {
    const session = this.readStoredSession();
    return session ? { username: session.username, role: session.role } : null;
  }

  private readStoredSession(): StoredSession | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as StoredSession;
    } catch {
      localStorage.removeItem(STORAGE_KEY); // bozuk kayıt → temizle
      return null;
    }
  }
}
