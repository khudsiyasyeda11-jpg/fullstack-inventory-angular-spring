import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AssetDetailComponent } from './asset-detail.component';
import { AssetService } from '../../core/asset.service';
import { makeAsset } from '../../core/testing';

describe('AssetDetailComponent', () => {
  const service = { get: vi.fn(), delete: vi.fn() };

  beforeEach(async () => {
    service.get.mockReset();
    service.delete.mockReset();
    await TestBed.configureTestingModule({
      imports: [AssetDetailComponent],
      providers: [provideRouter([]), { provide: AssetService, useValue: service }],
    }).compileComponents();
  });

  async function render(id = '3') {
    const fixture = TestBed.createComponent(AssetDetailComponent);
    fixture.componentRef.setInput('id', id);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  it('renders asset fields', async () => {
    service.get.mockReturnValue(of(makeAsset({ id: 3, name: 'Van 3', category: 'VEHICLE', status: 'IN_REPAIR' })));
    const fixture = await render();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('h1')?.textContent).toContain('Van 3');
    expect(el.querySelector('.badge')?.textContent).toContain('In Repair');
  });

  it('shows the problem title when the asset is missing', async () => {
    service.get.mockReturnValue(throwError(() => ({ title: 'Asset not found', status: 404 })));
    const fixture = await render('404');
    expect((fixture.nativeElement as HTMLElement).querySelector('.alert')?.textContent).toContain('Asset not found');
  });

  it('deletes after confirmation and returns to the list', async () => {
    service.get.mockReturnValue(of(makeAsset({ id: 3 })));
    service.delete.mockReturnValue(of(undefined));
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = await render();

    fixture.componentInstance.remove(fixture.componentInstance.asset()!);

    expect(service.delete).toHaveBeenCalledWith(3);
    expect(navigate).toHaveBeenCalledWith(['/assets']);
  });
});
