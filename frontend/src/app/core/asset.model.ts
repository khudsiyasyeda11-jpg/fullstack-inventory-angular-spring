export const ASSET_CATEGORIES = [
  'LAPTOP',
  'MONITOR',
  'PHONE',
  'TOOL',
  'VEHICLE',
  'FURNITURE',
  'OTHER',
] as const;
export type AssetCategory = (typeof ASSET_CATEGORIES)[number];

export const ASSET_STATUSES = ['IN_STOCK', 'ASSIGNED', 'IN_REPAIR', 'RETIRED'] as const;
export type AssetStatus = (typeof ASSET_STATUSES)[number];

export interface Asset {
  id: number;
  assetTag: string;
  name: string;
  category: AssetCategory;
  status: AssetStatus;
  location: string;
  assignedTo: string | null;
  purchaseDate: string | null;
  purchaseCost: number | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Payload for create / update (server-managed fields omitted). */
export type AssetRequest = Omit<Asset, 'id' | 'createdAt' | 'updatedAt'>;

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AssetQuery {
  q?: string;
  category?: AssetCategory | '';
  status?: AssetStatus | '';
  page?: number;
  size?: number;
  sort?: string;
}

export type StatusSummary = Record<AssetStatus, number>;

/** Subset of RFC 9457 problem details returned by the API. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  errors?: Record<string, string>;
}

export function label(value: string): string {
  return value
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
}
