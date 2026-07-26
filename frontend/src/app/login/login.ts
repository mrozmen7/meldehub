import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { ApiError } from '../models/case.model';

/**
 * Giriş ekranı (CASE-201) — POST /api/auth/login.
 * Başarıda returnUrl'e (yoksa role göre varsayılana) yönlendirir;
 * hatalı girişte backend'in {"error": "..."} mesajını kırmızı gösterir.
 */
@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  onSubmit(): void {
    this.errorMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const { username, password } = this.form.getRawValue();

    this.auth.login(username, password).subscribe({
      next: (res) => {
        this.submitting.set(false);
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        // Varsayılan yön: operatör panele, vatandaş ihbar formuna
        const fallback = res.role === 'OPERATOR' ? '/operator' : '/report';
        void this.router.navigateByUrl(returnUrl ?? fallback);
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        const apiError = err.error as ApiError | null;
        this.errorMessage.set(apiError?.error ?? 'Giriş yapılamadı. Lütfen tekrar deneyiniz.');
      },
    });
  }
}
