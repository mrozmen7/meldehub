import { Routes } from '@angular/router';

import { Report } from './report/report';
import { Operator } from './operator/operator';

export const routes: Routes = [
  { path: 'report', component: Report, title: 'MeldeHub — İhbar Ver' },
  { path: 'operator', component: Operator, title: 'MeldeHub — Operatör Paneli' },
  { path: '', pathMatch: 'full', redirectTo: 'report' },
  { path: '**', redirectTo: 'report' },
];
