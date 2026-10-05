import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { API_BASE, AssetService } from './asset.service';
import { Asset, AssetRequest, ProblemDetail } from './asset.model';

const sample: Asset = {
  id: 1,
  assetTag: 'AST-0001',
  name: 'Rugged Laptop 14',
  category: 'LAPTOP',
  status: 'IN_STOCK',
  location: 'Depot North',
  assignedTo: null,
  purchaseDate: '2025-01-15',
  purchaseCost: 1450,
  notes: null,
  createdAt: '2025-01-15T10:00:00Z',
  updatedAt: '2025-01-15T10:00:00Z',
};

describe('AssetService', () => {
  let service: AssetService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AssetService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends only non-empty query params when listing', () => {
    service.list({ q: 'laptop', category: '', status: 'ASSIGNED', page: 0, size: 10 }).subscribe((page) => {
      expect(page.totalElements).toBe(1);
    });

    const req = http.expectOne((r) => r.url === API_BASE);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('q')).toBe('laptop');
    expect(req.request.params.get('status')).toBe('ASSIGNED');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.has('category')).toBe(false);
    req.flush({ content: [sample], page: 0, size: 10, totalElements: 1, totalPages: 1 });
  });

  it('posts a create request', () => {
    const { id, createdAt, updatedAt, ...request } = sample;
    service.create(request as AssetRequest).subscribe((created) => expect(created.id).toBe(1));

    const req = http.expectOne(API_BASE);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.assetTag).toBe('AST-0001');
    req.flush(sample);
  });

  it('puts an update and deletes by id', () => {
    const { id, createdAt, updatedAt, ...request } = sample;
    service.update(1, request as AssetRequest).subscribe();
    http.expectOne({ method: 'PUT', url: `${API_BASE}/1` }).flush(sample);

    service.delete(1).subscribe();
    http.expectOne({ method: 'DELETE', url: `${API_BASE}/1` }).flush(null, { status: 204, statusText: 'No Content' });
  });

  it('passes through RFC 9457 problem details from the API', () => {
    let problem: ProblemDetail | undefined;
    service.get(99).subscribe({ error: (p: ProblemDetail) => (problem = p) });

    http
      .expectOne(`${API_BASE}/99`)
      .flush({ title: 'Asset not found', status: 404, detail: 'Asset 99 does not exist' }, { status: 404, statusText: 'Not Found' });

    expect(problem?.title).toBe('Asset not found');
    expect(problem?.status).toBe(404);
  });

  it('maps network failures to a generic problem', () => {
    let problem: ProblemDetail | undefined;
    service.summary().subscribe({ error: (p: ProblemDetail) => (problem = p) });

    http.expectOne(`${API_BASE}/summary`).error(new ProgressEvent('error'), { status: 0 });

    expect(problem?.title).toBe('Network error');
  });
});
