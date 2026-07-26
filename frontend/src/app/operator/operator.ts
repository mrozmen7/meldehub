import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';

import { CaseService } from '../services/case.service';
import {
  ApiError,
  CASE_STATUSES,
  CATEGORY_LABELS,
  CaseResponse,
  CaseStatus,
  STATUS_LABELS,
} from '../models/case.model';

/** Operatör paneli — vaka listesi + durum yönetimi */
@Component({
  selector: 'app-operator',
  imports: [DatePipe, RouterLink],
  templateUrl: './operator.html',
  styleUrl: './operator.css',
})
export class Operator implements OnInit {
  private readonly caseService = inject(CaseService);

  readonly statuses = CASE_STATUSES;
  readonly statusLabels = STATUS_LABELS;
  readonly categoryLabels = CATEGORY_LABELS;

  readonly cases = signal<CaseResponse[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  /** Vaka ID'sine göre geçiş hatası (409 mesajları). */
  readonly transitionErrors = signal<Record<string, string>>({});
  /** Durum güncellemesi devam eden vakalar. */
  readonly updating = signal<Record<string, boolean>>({});

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.caseService.getCases().subscribe({
      next: (cases) => {
        this.cases.set(cases);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('Vakalar yüklenemedi. Backend çalışıyor mu? (http://localhost:8080)');
        this.loading.set(false);
      },
    });
  }

  onStatusChange(c: CaseResponse, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const next = select.value as CaseStatus;
    if (next === c.status) return;

    this.updating.update((m) => ({ ...m, [c.id]: true }));
    this.transitionErrors.update((m) => {
      const copy = { ...m };
      delete copy[c.id];
      return copy;
    });

    this.caseService.updateStatus(c.id, next).subscribe({
      next: (updated) => {
        this.cases.update((list) => list.map((x) => (x.id === updated.id ? updated : x)));
        this.updating.update((m) => ({ ...m, [c.id]: false }));
      },
      error: (err: HttpErrorResponse) => {
        // 409: backend {"error": "Geçersiz durum geçişi: X → Y"} döner — bilinçli gösteriyoruz.
        const apiError = err.error as ApiError | null;
        this.transitionErrors.update((m) => ({
          ...m,
          [c.id]: apiError?.error ?? 'Durum güncellenemedi.',
        }));
        this.updating.update((m) => ({ ...m, [c.id]: false }));
        // Dropdown'u gerçek duruma geri al
        select.value = c.status;
      },
    });
  }

  statusClass(status: CaseStatus): string {
    return `badge status-${status.toLowerCase().replace('_', '-')}`;
  }
}
