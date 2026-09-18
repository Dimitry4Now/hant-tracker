import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './core/guards';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/public/public.component').then((m) => m.PublicComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register.component').then((m) => m.RegisterComponent)
  },
  {
    path: '',
    loadComponent: () => import('./layout/shell.component').then((m) => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent)
      },
      {
        path: 'analysis',
        loadComponent: () =>
          import('./features/analysis/analysis.component').then((m) => m.AnalysisComponent)
      },
      {
        path: 'account',
        loadComponent: () =>
          import('./features/account/account.component').then((m) => m.AccountComponent)
      },
      {
        path: 'admin',
        canActivate: [adminGuard],
        children: [
          { path: '', pathMatch: 'full', redirectTo: 'requests' },
          {
            path: 'requests',
            loadComponent: () =>
              import('./features/admin/requests.component').then((m) => m.RequestsComponent)
          },
          {
            path: 'games',
            loadComponent: () =>
              import('./features/admin/games.component').then((m) => m.GamesComponent)
          },
          {
            path: 'games/:id',
            loadComponent: () =>
              import('./features/admin/game-detail.component').then((m) => m.GameDetailComponent)
          },
          {
            path: 'users',
            loadComponent: () =>
              import('./features/admin/users.component').then((m) => m.UsersComponent)
          }
        ]
      }
    ]
  },
  { path: '**', redirectTo: '' }
];
