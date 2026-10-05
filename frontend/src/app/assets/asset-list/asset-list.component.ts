import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { AssetService } from '../../core/asset.service';
import {
  ASSET_CATEGORIES,
  ASSET_STATUSES,
  Asset,
  AssetCategory,
  AssetStatus,
  PageResponse,
  ProblemDetail,
  StatusSummary,
  label,
} from '../../core/asset.model';

type SortField = 'assetTag' | 'name' | 'category' | 'status' | 'location' | 'purchaseDate';

@Component({
  selector: 'app-asset-list',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe],
  templateUrl: './asset-list.component.html',
  styleUrl: './asset-list.component.css',
})
export class AssetListComponent implements OnInit {
  private readonly service = inject(AssetService);
  private readonly destroyRef = inject(DestroyRef);

  readonly categories = ASSET_CATEGORIES;
  readonly statuses = ASSET_STATUSES;
  readonly label = label;
  readonly pageSize = 10;

  readonly filters = inject(NonNullableFormBuilder).group({
    q: '',
    category: '' as AssetCategory | '',
    status: '' as AssetStatus | '',
  });

  readonly page = signal<PageResponse<Asset> | null>(null);
  readonly summary = signal<StatusSummary | null>(null);
  readonly loading = signal(false);
  readonly error = signal<ProblemDetail | null>(null);
  readonly sortField = signal<SortField>('assetTag');
  readonly sortDir = signal<'asc' | 'desc'>('asc');
  readonly pageIndex = signal(0);

  ngOnInit(): void {
    this.filters.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.pageIndex.set(0);
        this.load();
      });
    this.load();
    this.loadSummary();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    const { q, category, status } = this.filters.getRawValue();
    this.service
      .list({ q, category, status, page: this.pageIndex(), size: this.pageSize, sort: `${this.sortField()},${this.sortDir()}` })
      .subscribe({
        next: (page) => {
          this.page.set(page);
          this.loading.set(false);
        },
        error: (problem: ProblemDetail) => {
          this.error.set(problem);
          this.loading.set(false);
        },
      });
  }

  loadSummary(): void {
    this.service.summary().subscribe({ next: (s) => this.summary.set(s), error: () => this.summary.set(null) });
  }

  sortBy(field: SortField): void {
    if (this.sortField() === field) {
      this.sortDir.set(this.sortDir() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortField.set(field);
      this.sortDir.set('asc');
    }
    this.load();
  }

  sortIndicator(field: SortField): string {
    if (this.sortField() !== field) return '';
    return this.sortDir() === 'asc' ? '▲' : '▼';
  }

  goTo(index: number): void {
    const total = this.page()?.totalPages ?? 0;
    if (index < 0 || index >= total) return;
    this.pageIndex.set(index);
    this.load();
  }

  filterByStatus(status: AssetStatus): void {
    this.filters.controls.status.setValue(this.filters.controls.status.value === status ? '' : status);
  }

  clearFilters(): void {
    this.filters.reset();
  }
}
