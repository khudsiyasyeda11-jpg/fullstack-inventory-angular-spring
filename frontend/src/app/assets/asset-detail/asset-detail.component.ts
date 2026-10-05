import { Component, OnInit, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { AssetService } from '../../core/asset.service';
import { Asset, ProblemDetail, label } from '../../core/asset.model';

@Component({
  selector: 'app-asset-detail',
  imports: [RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './asset-detail.component.html',
})
export class AssetDetailComponent implements OnInit {
  private readonly service = inject(AssetService);
  private readonly router = inject(Router);

  /** Bound from the `:id` route parameter via withComponentInputBinding(). */
  readonly id = input.required<string>();

  readonly asset = signal<Asset | null>(null);
  readonly error = signal<ProblemDetail | null>(null);
  readonly label = label;

  ngOnInit(): void {
    this.service.get(Number(this.id())).subscribe({
      next: (a) => this.asset.set(a),
      error: (p: ProblemDetail) => this.error.set(p),
    });
  }

  remove(asset: Asset): void {
    if (!confirm(`Delete ${asset.assetTag}? This cannot be undone.`)) return;
    this.service.delete(asset.id).subscribe({
      next: () => this.router.navigate(['/assets']),
      error: (p: ProblemDetail) => this.error.set(p),
    });
  }
}
