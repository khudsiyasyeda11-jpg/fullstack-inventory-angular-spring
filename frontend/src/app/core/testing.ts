import { Asset, PageResponse } from './asset.model';

export function makeAsset(overrides: Partial<Asset> = {}): Asset {
  return {
    id: 1,
    assetTag: 'LAP-1001',
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
    ...overrides,
  };
}

export function pageOf(content: Asset[], page = 0, totalPages = 1): PageResponse<Asset> {
  return { content, page, size: 10, totalElements: content.length, totalPages };
}
