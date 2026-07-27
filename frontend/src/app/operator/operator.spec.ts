import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { Operator } from './operator';
import { CaseResponse, Page } from '../models/case.model';

/**
 * CASE-233 — operatör paneli sayfalama + durum filtresi davranışı.
 * HTTP istekleri HttpTestingController ile doğrulanır (gerçek ağ yok).
 */
describe('Operator (sayfalama + filtre)', () => {
  let httpMock: HttpTestingController;

  const sampleCase: CaseResponse = {
    id: '3fa85f64-5717-4562-b3fc-2c963f66afa6',
    title: 'Kaldırımda büyük çukur',
    description: 'Bahnhofstrasse 12 önünde tehlikeli çukur var.',
    category: 'POTHOLE',
    status: 'NEW',
    location: 'Bahnhofstrasse 12, Zürich',
    reporterEmail: 'vatandas@example.ch',
    createdAt: '2026-07-25T10:00:00Z',
    updatedAt: '2026-07-25T10:00:00Z',
  };

  function pageOf(content: CaseResponse[], number: number, totalElements: number, totalPages: number): Page<CaseResponse> {
    return { content, totalElements, totalPages, number, size: 20 };
  }

  function createComponent() {
    const fixture = TestBed.createComponent(Operator);
    const component = fixture.componentInstance;
    fixture.detectChanges(); // ngOnInit → ilk GET tetiklenir
    return { fixture, component };
  }

  function flushInitial(pageData: Page<CaseResponse>) {
    const req = httpMock.expectOne('/api/cases?page=0&size=20');
    expect(req.request.method).toBe('GET');
    req.flush(pageData);
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Operator],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('açılışta ilk sayfayı 20 boyutla yüklemeli', () => {
    const { component } = createComponent();
    flushInitial(pageOf([sampleCase], 0, 1, 1));

    expect(component.cases().length).toBe(1);
    expect(component.totalElements()).toBe(1);
    expect(component.totalPages()).toBe(1);
    expect(component.page()).toBe(0);
  });

  it('sonraki sayfa isteği page=1 göndermeli', () => {
    const { component } = createComponent();
    flushInitial(pageOf([sampleCase], 0, 30, 2));

    component.nextPage();
    const req = httpMock.expectOne('/api/cases?page=1&size=20');
    req.flush(pageOf([{ ...sampleCase, id: 'b' }], 1, 30, 2));

    expect(component.page()).toBe(1);
  });

  it('önceki sayfa isteği page=0 göndermeli', () => {
    const { component } = createComponent();
    flushInitial(pageOf([sampleCase], 0, 30, 2));

    component.nextPage();
    httpMock.expectOne('/api/cases?page=1&size=20').flush(pageOf([sampleCase], 1, 30, 2));

    component.previousPage();
    httpMock.expectOne('/api/cases?page=0&size=20').flush(pageOf([sampleCase], 0, 30, 2));

    expect(component.page()).toBe(0);
  });

  it('ilk sayfadayken önceki butonu istek atmamalı', () => {
    const { component } = createComponent();
    flushInitial(pageOf([sampleCase], 0, 30, 2));

    component.previousPage();
    httpMock.expectNone((r) => r.url === '/api/cases');
    expect(component.page()).toBe(0);
  });

  it('son sayfadayken sonraki butonu istek atmamalı', () => {
    const { component } = createComponent();
    flushInitial(pageOf([sampleCase], 0, 30, 2));

    component.nextPage();
    httpMock.expectOne('/api/cases?page=1&size=20').flush(pageOf([sampleCase], 1, 30, 2));

    component.nextPage(); // zaten son sayfadayız
    httpMock.expectNone((r) => r.url === '/api/cases');
    expect(component.page()).toBe(1);
  });

  it('durum filtresi seçilince status parametresi gönderilmeli', () => {
    const { component } = createComponent();
    flushInitial(pageOf([sampleCase], 0, 1, 1));

    const event = { target: { value: 'NEW' } } as unknown as Event;
    component.onFilterChange(event);

    const req = httpMock.expectOne('/api/cases?page=0&size=20&status=NEW');
    expect(req.request.method).toBe('GET');
    req.flush(pageOf([sampleCase], 0, 1, 1));

    expect(component.statusFilter()).toBe('NEW');
  });

  it('filtre değişince sayfa sıfırlanmalı', () => {
    const { component } = createComponent();
    flushInitial(pageOf([sampleCase], 0, 30, 2));

    component.nextPage();
    httpMock.expectOne('/api/cases?page=1&size=20').flush(pageOf([sampleCase], 1, 30, 2));
    expect(component.page()).toBe(1);

    component.onFilterChange({ target: { value: 'CLOSED' } } as unknown as Event);

    expect(component.page()).toBe(0);
    httpMock
      .expectOne('/api/cases?page=0&size=20&status=CLOSED')
      .flush(pageOf([], 0, 0, 0));
  });

  it('"Tümü" seçilince status parametresi gönderilmemeli', () => {
    const { component } = createComponent();
    flushInitial(pageOf([sampleCase], 0, 1, 1));

    component.onFilterChange({ target: { value: 'NEW' } } as unknown as Event);
    httpMock.expectOne('/api/cases?page=0&size=20&status=NEW').flush(pageOf([sampleCase], 0, 1, 1));

    component.onFilterChange({ target: { value: '' } } as unknown as Event);
    httpMock.expectOne('/api/cases?page=0&size=20').flush(pageOf([sampleCase], 0, 1, 1));

    expect(component.statusFilter()).toBe('');
  });

  it('yükleme hatasında Türkçe hata mesajı gösterilmeli', () => {
    const { component } = createComponent();
    httpMock
      .expectOne('/api/cases?page=0&size=20')
      .flush('hata', { status: 500, statusText: 'Server Error' });

    expect(component.loadError()).toContain('yüklenemedi');
    expect(component.loading()).toBe(false);
  });

  it('sayfa bilgisi metni toplam vaka sayısını içermeli', async () => {
    const { fixture } = createComponent();
    flushInitial(pageOf([sampleCase], 0, 42, 3));
    await fixture.whenStable();
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.page-info')?.textContent).toContain('Sayfa 1 / 3');
    expect(el.querySelector('.page-info')?.textContent).toContain('42 vaka');
  });

  it('filtre dropdownu Tümü + 5 durum seçeneği içermeli', async () => {
    const { fixture } = createComponent();
    flushInitial(pageOf([sampleCase], 0, 1, 1));
    await fixture.whenStable();
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    const options = Array.from(
      el.querySelectorAll('#status-filter option')
    ).map((o) => o.textContent?.trim());
    expect(options).toEqual(['Tümü', 'Yeni', 'Sınıflandırıldı', 'İşlemde', 'Çözüldü', 'Kapatıldı']);
  });
});
