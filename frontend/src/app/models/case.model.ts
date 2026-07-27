/**
 * Backend ile birebir eşleşen model tanımları.
 * Kaynak: backend/src/main/java/ch/meldehub/domain ve api paketleri.
 */

/** CaseCategory enum — backend: ch.meldehub.domain.CaseCategory */
export type CaseCategory = 'POTHOLE' | 'LIGHTING' | 'WASTE' | 'NOISE' | 'OTHER';

export const CASE_CATEGORIES: CaseCategory[] = ['POTHOLE', 'LIGHTING', 'WASTE', 'NOISE', 'OTHER'];

/** CaseStatus enum — backend: ch.meldehub.domain.CaseStatus (NEW → TRIAGED → IN_PROGRESS → RESOLVED → CLOSED) */
export type CaseStatus = 'NEW' | 'TRIAGED' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';

export const CASE_STATUSES: CaseStatus[] = ['NEW', 'TRIAGED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

/** POST /api/cases istek gövdesi — backend: CaseCreateRequest record */
export interface CaseCreateRequest {
  title: string;
  description: string;
  category: CaseCategory;
  location: string;
  reporterEmail: string;
}

/** API cevabı — backend: CaseResponse record */
export interface CaseResponse {
  id: string;
  title: string;
  description: string;
  category: CaseCategory;
  status: CaseStatus;
  location: string;
  reporterEmail: string;
  createdAt: string;
  updatedAt: string;
}

/** PATCH /api/cases/{id}/status istek gövdesi — backend: StatusUpdateRequest record */
export interface StatusUpdateRequest {
  status: CaseStatus;
}

/** Hata cevapları — backend: GlobalExceptionHandler { "error": "..." } */
export interface ApiError {
  error: string;
}

/**
 * Spring Data Page JSON'u — backend CASE-233: GET /api/cases artık Page<CaseResponse> döner.
 * Birebir karşılığı: org.springframework.data.domain.Page serileştirmesi.
 */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  /** 0-tabanlı sayfa numarası */
  number: number;
  size: number;
}

/** UI etiketleri (Türkçe) */
export const CATEGORY_LABELS: Record<CaseCategory, string> = {
  POTHOLE: 'Yol Çukuru',
  LIGHTING: 'Aydınlatma Arızası',
  WASTE: 'Çöp / Atık',
  NOISE: 'Gürültü Şikâyeti',
  OTHER: 'Diğer',
};

export const STATUS_LABELS: Record<CaseStatus, string> = {
  NEW: 'Yeni',
  TRIAGED: 'Sınıflandırıldı',
  IN_PROGRESS: 'İşlemde',
  RESOLVED: 'Çözüldü',
  CLOSED: 'Kapatıldı',
};
