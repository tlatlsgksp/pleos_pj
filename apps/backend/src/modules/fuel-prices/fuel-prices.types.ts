import type { BrandCode, ProductCode } from './opinet/opinet.types.js';

/** Module-owned shapes the app receives: numbers as numbers, coordinates in WGS84. */
export interface ProductAverage {
  product: ProductCode;
  productName: string;
  /** won per litre */
  price: number;
  /** change from the previous day, won */
  diff: number;
  /** yyyy-MM-dd */
  date: string;
}

export interface NearbyStation {
  id: string;
  name: string;
  brand: BrandCode;
  price: number;
  distanceM: number;
  lat: number;
  lng: number;
}

export interface StationPrice {
  product: ProductCode;
  price: number;
  /** ISO local date-time of the price report, e.g. 2026-11-05T16:00:00 */
  reportedAt: string;
}

export interface StationDetail {
  id: string;
  name: string;
  brand: BrandCode | null;
  address: string;
  tel: string;
  isLpg: boolean;
  hasCarWash: boolean;
  hasMaintenance: boolean;
  hasConvenienceStore: boolean;
  lat: number;
  lng: number;
  prices: StationPrice[];
}

export type NearbySort = 'price' | 'distance';
