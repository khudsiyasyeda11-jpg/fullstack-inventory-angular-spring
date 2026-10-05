import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AssetListComponent } from './asset-list.component';
import { AssetService } from '../../core/asset.service';
import { makeAsset, pageOf } from '../../core/testing';

describe('AssetListComponent', () => {
  const service = {
    list: vi.fn(),
    summary: vi.fn(),
  };

  beforeEach(async () => {
    service.list.mockReset().mockReturnValue(
      of(pageOf([makeAsset(), makeAsset({ id: 2, assetTag: 'MON-2001', name: 'Monitor 27', category: 'MONITOR', status: 'ASSIGNED', assignedTo: 'Crew 7' })], 0, 3)),
    );
    service.summary.mockReset().mockReturnValue(of({ IN_STOCK: 5, ASSIGNED: 3, IN_REPAIR: 1, RETIRED: 2 }));
    await TestBed.configureTestingModule({
      imports: [AssetListComponent],
      providers: [provideRouter([]), { provide: AssetService, useValue: service }],
    }).compileComponents();
  });

  async function render() {
    const fixture = TestBed.createComponent(AssetListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  it('loads the first page sorted by tag and renders rows and summary', async () => {
    const fixture = await render();
    const el = fixture.nativeElement as HTMLElement;

    expect(service.list).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 10, sort: 'assetTag,asc' }));
    expect(el.querySelectorAll('tbody tr').length).toBe(2);
    expect(el.textContent).toContain('MON-2001');
    expect(el.querySelectorAll('.summary-card').length).toBe(4);
    expect(el.querySelector('.pager')?.textContent).toContain('Page 1 of 3');
  });

  it('toggles sort direction when the same header is clicked twice', async () => {
    const fixture = await render();
    const component = fixture.componentInstance;

    component.sortBy('name');
    expect(service.list).toHaveBeenLastCalledWith(expect.objectContaining({ sort: 'name,asc' }));
    component.sortBy('name');
    expect(service.list).toHaveBeenLastCalledWith(expect.objectContaining({ sort: 'name,desc' }));
  });

  it('moves between pages within bounds', async () => {
    const fixture = await render();
    const component = fixture.componentInstance;

    component.goTo(1);
    expect(service.list).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }));
    const calls = service.list.mock.calls.length;
    component.goTo(-1);
    expect(service.list.mock.calls.length).toBe(calls);
  });

  it('reloads from page 0 with filters after the debounce', async () => {
    const fixture = await render();
    const component = fixture.componentInstance;
    component.pageIndex.set(2);

    component.filterByStatus('IN_REPAIR');
    await new Promise((resolve) => setTimeout(resolve, 300));

    expect(service.list).toHaveBeenLastCalledWith(expect.objectContaining({ status: 'IN_REPAIR', page: 0 }));
  });
});
