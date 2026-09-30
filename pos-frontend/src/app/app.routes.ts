import { Routes } from '@angular/router';

/**
 * Every feature is lazily loaded, so opening the till does not download the reports code.
 * The terminal is the default route because it is what staff use all day.
 */
export const routes: Routes = [
  {
    path: 'terminal',
    title: 'Terminal',
    loadComponent: () => import('./features/terminal/terminal').then((m) => m.Terminal),
  },
  {
    path: 'dashboard',
    title: 'Dashboard',
    loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
  },
  {
    path: 'products',
    title: 'Products',
    loadComponent: () => import('./features/products/products').then((m) => m.Products),
  },
  {
    path: 'categories',
    title: 'Categories',
    loadComponent: () => import('./features/categories/categories').then((m) => m.Categories),
  },
  {
    path: 'customers',
    title: 'Customers',
    loadComponent: () => import('./features/customers/customers').then((m) => m.Customers),
  },
  {
    path: 'sales',
    title: 'Sales',
    loadComponent: () => import('./features/sales/sales').then((m) => m.Sales),
  },
  { path: '', pathMatch: 'full', redirectTo: 'terminal' },
  { path: '**', redirectTo: 'terminal' },
];
