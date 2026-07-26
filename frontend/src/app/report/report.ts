import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { CaseService } from '../services/case.service';
import {
  ApiError,
  CASE_CATEGORIES,
  CATEGORY_LABELS,
  CaseCategory,
  CaseCreateRequest,
  CaseResponse,
} from '../models/case.model';

/** Vatandaş ihbar formu — POST /api/cases */
@Component({
  selector: 'app-report',
  imports: [ReactiveFormsModule],
  templateUrl: './report.html',
  styleUrl: './report.css',
})
export class Report {
  private readonly fb = inject(FormBuilder);
  private readonly caseService = inject(CaseService);

  readonly categories = CASE_CATEGORIES;
  readonly categoryLabels = CATEGORY_LABELS;

  readonly submitting = signal(false);
  readonly successCase = signal<CaseResponse | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    description: ['', [Validators.required, Validators.maxLength(2000)]],
    category: ['' as CaseCategory | '', Validators.required],
    location: ['', Validators.required],
    reporterEmail: ['', [Validators.required, Validators.email]],
  });

  /** Alan hatası var mı ve kullanıcıya gösterilmeli mi? */
  hasError(field: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[field];
    return control.invalid && (control.dirty || control.touched);
  }

  /** Alanın ilk hatasına karşılık gelen Türkçe mesaj. */
  errorText(field: keyof typeof this.form.controls): string {
    const control = this.form.controls[field];
    if (control.hasError('required')) return 'Bu alan zorunludur.';
    if (control.hasError('email')) return 'Geçerli bir e-posta adresi giriniz.';
    if (control.hasError('maxlength')) return 'Açıklama en fazla 2000 karakter olabilir.';
    return '';
  }

  onSubmit(): void {
    this.errorMessage.set(null);
    this.successCase.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const request = this.form.getRawValue() as CaseCreateRequest;

    this.caseService.createCase(request).subscribe({
      next: (created) => {
        this.submitting.set(false);
        this.successCase.set(created);
        this.form.reset();
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        const apiError = err.error as ApiError | null;
        this.errorMessage.set(
          apiError?.error ?? 'İhbar gönderilemedi. Lütfen daha sonra tekrar deneyiniz.'
        );
      },
    });
  }
}
