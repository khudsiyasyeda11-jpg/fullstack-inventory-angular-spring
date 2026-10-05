import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'assets' },
  {
    path: 'assets',
    title: 'Assets',
    loadComponent: () => import('./assets/asset-list/asset-list.component').then((m) => m.AssetListComponent),
  },
  {
    path: 'assets/new',
    title: 'New asset',
    loadComponent: () => import('./assets/asset-form/asset-form.component').then((m) => m.AssetFormComponent),
  },
  {
    path: 'assets/:id',
    title: 'Asset details',
    loadComponent: () => import('./assets/asset-detail/asset-detail.component').then((m) => m.AssetDetailComponent),
  },
  {
    path: 'assets/:id/edit',
    title: 'Edit asset',
    loadComponent: () => import('./assets/asset-form/asset-form.component').then((m) => m.AssetFormComponent),
  },
  { path: '**', redirectTo: 'assets' },
];
