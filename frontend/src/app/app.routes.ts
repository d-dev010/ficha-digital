import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'clientes', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'clientes',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./features/clientes/clientes-busca/clientes-busca.component').then(m => m.ClientesBuscaComponent)
      },
      {
        path: ':id',
        loadComponent: () => import('./features/clientes/cliente-extrato/cliente-extrato.component').then(m => m.ClienteExtratoComponent)
      }
    ]
  },
  {
    path: 'dashboard',
    canActivate: [authGuard, roleGuard('DONO')],
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  { path: '**', redirectTo: 'clientes' }
];
