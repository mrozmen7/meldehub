import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { Report } from './report';
import { CaseResponse } from '../models/case.model';

describe('Report (ihbar formu)', () => {
  let httpMock: HttpTestingController;

  const createdCase: CaseResponse = {
    id: '3fa85f64-5717-4562-b3fc-2c963f66afa6',
    title: 'Sokak lambası yanmıyor',
    description: 'Üç gündür çalışmıyor.',
    category: 'LIGHTING',
    status: 'NEW',
    location: 'Seefeldstrasse 5, Zürich',
    reporterEmail: 'vatandas@example.ch',
    createdAt: '2026-07-25T10:00:00Z',
    updatedAt: '2026-07-25T10:00:00Z',
  };

  function createComponent() {
    const fixture = TestBed.createComponent(Report);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    return { fixture, component };
  }

  function fillValidForm(component: Report) {
    component.form.setValue({
      title: 'Sokak lambası yanmıyor',
      description: 'Üç gündür çalışmıyor.',
      category: 'LIGHTING',
      location: 'Seefeldstrasse 5, Zürich',
      reporterEmail: 'vatandas@example.ch',
    });
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Report],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('başlangıçta form geçersiz olmalı', () => {
    const { component } = createComponent();
    expect(component.form.invalid).toBe(true);
  });

  it('boş form gönderildiğinde istek atılmamalı ve alanlar touched olmalı', () => {
    const { component } = createComponent();

    component.onSubmit();

    expect(component.form.controls.title.touched).toBe(true);
    expect(component.form.controls.reporterEmail.touched).toBe(true);
    httpMock.expectNone('/api/cases');
  });

  it('geçersiz e-posta ile form geçersiz kalmalı', () => {
    const { component } = createComponent();
    fillValidForm(component);
    component.form.controls.reporterEmail.setValue('gecersiz-eposta');

    expect(component.form.invalid).toBe(true);
    component.onSubmit();
    httpMock.expectNone('/api/cases');
  });

  it('zorunlu alan boşsa Türkçe hata metni dönmeli', () => {
    const { component } = createComponent();
    component.form.controls.title.markAsTouched();

    expect(component.hasError('title')).toBe(true);
    expect(component.errorText('title')).toBe('Bu alan zorunludur.');
  });

  it('geçerli form gönderildiğinde POST atılmalı ve başarı durumunda form temizlenmeli', () => {
    const { component } = createComponent();
    fillValidForm(component);
    expect(component.form.valid).toBe(true);

    component.onSubmit();

    const req = httpMock.expectOne('/api/cases');
    expect(req.request.method).toBe('POST');
    req.flush(createdCase);

    expect(component.successCase()?.id).toBe(createdCase.id);
    expect(component.successCase()?.status).toBe('NEW');
    expect(component.form.controls.title.value).toBe('');
  });

  it('backend 400 dönerse gelen hata mesajı gösterilmeli', () => {
    const { component } = createComponent();
    fillValidForm(component);

    component.onSubmit();

    const req = httpMock.expectOne('/api/cases');
    req.flush(
      { error: 'reporterEmail: must be a well-formed email address' },
      { status: 400, statusText: 'Bad Request' }
    );

    expect(component.errorMessage()).toBe('reporterEmail: must be a well-formed email address');
    expect(component.successCase()).toBeNull();
  });
});
