import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AssetFormComponent } from './asset-form.component';
import { AssetService } from '../../core/asset.service';
import { makeAsset } from '../../core/testing';

describe('AssetFormComponent', () => {
  const service = { get: vi.fn(), create: vi.fn(), update: vi.fn() };

  beforeEach(async () => {
    Object.values(service).forEach((fn) => fn.mockReset());
    await TestBed.configureTestingModule({
      imports: [AssetFormComponent],
      providers: [provideRouter([]), { provide: AssetService, useValue: service }],
    }).compileComponents();
  });

  function fillValid(component: AssetFormComponent) {
    component.form.setValue({
      assetTag: 'DRL-3001',
      name: 'Cordless drill',
      category: 'TOOL',
      status: 'IN_STOCK',
      location: 'Depot South',
      assignedTo: '',
      purchaseDate: '2025-03-01',
      purchaseCost: 189.5,
      notes: '',
    });
  }

  it('does not submit an invalid form and marks fields touched', () => {
    const fixture = TestBed.createComponent(AssetFormComponent);
    fixture.detectChanges();

    fixture.componentInstance.submit();

    expect(service.create).not.toHaveBeenCalled();
    expect(fixture.componentInstance.fieldError('assetTag')).toBe('Required');
  });

  it('validates the asset tag format', () => {
    const fixture = TestBed.createComponent(AssetFormComponent);
    const tag = fixture.componentInstance.form.controls.assetTag;
    tag.setValue('drill-1');
    tag.markAsTouched();
    expect(fixture.componentInstance.fieldError('assetTag')).toBe('Use the format ABC-1234');
    tag.setValue('DRL-3001');
    expect(tag.valid).toBe(true);
  });

  it('requires a holder when status is ASSIGNED', () => {
    const fixture = TestBed.createComponent(AssetFormComponent);
    const component = fixture.componentInstance;
    fillValid(component);
    component.form.controls.status.setValue('ASSIGNED');
    expect(component.form.hasError('assignedToRequired')).toBe(true);
    component.form.controls.assignedTo.setValue('Crew 4');
    expect(component.form.valid).toBe(true);
  });

  it('creates an asset with blank optional fields sent as null and navigates to it', () => {
    service.create.mockReturnValue(of(makeAsset({ id: 42 })));
    const fixture = TestBed.createComponent(AssetFormComponent);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    fillValid(fixture.componentInstance);

    fixture.componentInstance.submit();

    expect(service.create).toHaveBeenCalledWith(
      expect.objectContaining({ assetTag: 'DRL-3001', assignedTo: null, notes: null, purchaseCost: 189.5 }),
    );
    expect(navigate).toHaveBeenCalledWith(['/assets', 42]);
  });

  it('loads the asset in edit mode and surfaces server field errors', async () => {
    service.get.mockReturnValue(of(makeAsset({ id: 7, assetTag: 'LAP-1007' })));
    service.update.mockReturnValue(
      throwError(() => ({ title: 'Duplicate asset tag', status: 409, errors: { assetTag: 'already in use' } })),
    );
    const fixture = TestBed.createComponent(AssetFormComponent);
    fixture.componentRef.setInput('id', '7');
    fixture.detectChanges();
    await fixture.whenStable();

    const component = fixture.componentInstance;
    expect(service.get).toHaveBeenCalledWith(7);
    expect(component.form.controls.assetTag.value).toBe('LAP-1007');

    component.submit();
    expect(service.update).toHaveBeenCalledWith(7, expect.objectContaining({ assetTag: 'LAP-1007' }));
    expect(component.problem()?.title).toBe('Duplicate asset tag');
    expect(component.fieldError('assetTag')).toBe('already in use');
  });
});
