import { Component, OnInit, inject, input, signal } from '@angular/core';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { AssetService } from '../../core/asset.service';
import { ASSET_CATEGORIES, ASSET_STATUSES, Asset, AssetCategory, AssetRequest, AssetStatus, ProblemDetail, label } from '../../core/asset.model';

export const ASSET_TAG_PATTERN = /^[A-Z]{3}-\d{4,6}$/;

/** An asset in ASSIGNED status must name a holder (crew, team or depot). */
export const assignedToRequired: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const status = group.get('status')?.value;
  const assignedTo = (group.get('assignedTo')?.value ?? '').trim();
  return status === 'ASSIGNED' && !assignedTo ? { assignedToRequired: true } : null;
};

@Component({
  selector: 'app-asset-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './asset-form.component.html',
  styleUrl: './asset-form.component.css',
})
export class AssetFormComponent implements OnInit {
  private readonly service = inject(AssetService);
  private readonly router = inject(Router);

  /** Present on /assets/:id/edit, absent on /assets/new. */
  readonly id = input<string>();

  readonly categories = ASSET_CATEGORIES;
  readonly statuses = ASSET_STATUSES;
  readonly label = label;
  readonly saving = signal(false);
  readonly problem = signal<ProblemDetail | null>(null);

  readonly form = inject(NonNullableFormBuilder).group(
    {
      assetTag: ['', [Validators.required, Validators.pattern(ASSET_TAG_PATTERN)]],
      name: ['', [Validators.required, Validators.maxLength(120)]],
      category: ['LAPTOP' as AssetCategory, Validators.required],
      status: ['IN_STOCK' as AssetStatus, Validators.required],
      location: ['', [Validators.required, Validators.maxLength(120)]],
      assignedTo: ['', Validators.maxLength(120)],
      purchaseDate: [''],
      purchaseCost: [null as number | null, Validators.min(0)],
      notes: ['', Validators.maxLength(1000)],
    },
    { validators: assignedToRequired },
  );

  get isEdit(): boolean {
    return !!this.id();
  }

  ngOnInit(): void {
    const id = this.id();
    if (id) {
      this.service.get(Number(id)).subscribe({
        next: (asset) => this.patch(asset),
        error: (p: ProblemDetail) => this.problem.set(p),
      });
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.problem.set(null);
    const request = this.toRequest();
    const id = this.id();
    const call$: Observable<Asset> = id ? this.service.update(Number(id), request) : this.service.create(request);
    call$.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.router.navigate(['/assets', saved.id]);
      },
      error: (p: ProblemDetail) => {
        this.saving.set(false);
        this.problem.set(p);
      },
    });
  }

  /** Client-side error first, then any field error returned by the API. */
  fieldError(name: keyof AssetRequest): string | null {
    const control = this.form.controls[name];
    if (control.touched && control.errors) {
      if (control.errors['required']) return 'Required';
      if (control.errors['pattern']) return 'Use the format ABC-1234';
      if (control.errors['maxlength']) return `Max ${control.errors['maxlength'].requiredLength} characters`;
      if (control.errors['min']) return 'Must be zero or more';
    }
    if (name === 'assignedTo' && this.form.hasError('assignedToRequired') && (control.touched || this.form.controls.status.dirty)) {
      return 'Required when status is Assigned';
    }
    return this.problem()?.errors?.[name] ?? null;
  }

  private patch(asset: Asset): void {
    this.form.setValue({
      assetTag: asset.assetTag,
      name: asset.name,
      category: asset.category,
      status: asset.status,
      location: asset.location,
      assignedTo: asset.assignedTo ?? '',
      purchaseDate: asset.purchaseDate ?? '',
      purchaseCost: asset.purchaseCost,
      notes: asset.notes ?? '',
    });
  }

  private toRequest(): AssetRequest {
    const v = this.form.getRawValue();
    const blankToNull = (s: string) => (s.trim() === '' ? null : s.trim());
    return {
      assetTag: v.assetTag.trim(),
      name: v.name.trim(),
      category: v.category,
      status: v.status,
      location: v.location.trim(),
      assignedTo: blankToNull(v.assignedTo),
      purchaseDate: blankToNull(v.purchaseDate),
      purchaseCost: v.purchaseCost === null || (v.purchaseCost as unknown) === '' ? null : Number(v.purchaseCost),
      notes: blankToNull(v.notes),
    };
  }
}
