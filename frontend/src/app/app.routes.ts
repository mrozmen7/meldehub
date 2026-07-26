import { Routes } from '@angular/router';

import { roleGuard } from './guards/auth.guard';
import { Login } from './login/login';
import { Report } from './report/report';
import { Operator } from './operator/operator';

export const routes: Routes = [
  { path: 'login', component: Login, title: 'MeldeHub — Giriş' },
  // Vatandaş da giriş yapar (kamu gerçekçiliği) — ihbar CITIZEN + OPERATOR'a açık
  {
    path: 'report',
    component: Report,
    title: 'MeldeHub — İhbar Ver',
    canActivate: [roleGuard('CITIZEN', 'OPERATOR')],
  },
  // Operatör paneli sadece OPERATOR rolüne
  {
    path: 'operator',
    component: Operator,
    title: 'MeldeHub — Operatör Paneli',
    canActivate: [roleGuard('OPERATOR')],
  },
  { path: '', pathMatch: 'full', redirectTo: 'report' },
  { path: '**', redirectTo: 'report' },
];
