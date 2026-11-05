import { Injectable } from '@nestjs/common';
import { NotFoundError } from '../../common/errors/app-error.js';
import type {
  NearbySort,
  NearbyStation,
  ProductAverage,
  StationDetail,
} from './fuel-prices.types.js';
import { fromKatec, toKatec } from './opinet/katec.js';
import { OpinetClient } from './opinet/opinet.client.js';
import type { ProductCode, StationDetailRow } from './opinet/opinet.types.js';

/** Opinet caps aroundAll at 5 km. */
export const MAX_RADIUS_M = 5000;

/**
 * Fuel prices for the app, on top of the Opinet client. Translates Opinet's shape (string
 * numbers, KATEC coordinates, yyyyMMdd dates) into the app's and never leaks the raw rows.
 */
@Injectable()
export class FuelPricesService {
  constructor(private readonly opinet: OpinetClient) {}

  /** National averages per product: the default price for a refuel estimate. */
  async nationalAverages(): Promise<ProductAverage[]> {
    const { RESULT } = await this.opinet.avgAllPrice();
    return RESULT.OIL.map((row) => ({
      product: row.PRODCD,
      productName: row.PRODNM,
      price: Number(row.PRICE),
      diff: Number(row.DIFF),
      date: isoDate(row.TRADE_DT),
    }));
  }

  /** Stations around a point that sell [product], cheapest or nearest first. */
  async nearby(
    lat: number,
    lng: number,
    radiusM: number,
    product: ProductCode,
    sort: NearbySort,
  ): Promise<NearbyStation[]> {
    const { x, y } = toKatec({ lat, lng });
    const { RESULT } = await this.opinet.aroundAll({
      x,
      y,
      radius: Math.min(radiusM, MAX_RADIUS_M),
      prodcd: product,
      sort: sort === 'price' ? 1 : 2,
    });
    return RESULT.OIL.map((row) => ({
      id: row.UNI_ID,
      name: row.OS_NM,
      brand: row.POLL_DIV_CD,
      price: Number(row.PRICE),
      distanceM: Math.round(Number(row.DISTANCE)),
      ...fromKatec({ x: Number(row.GIS_X_COOR), y: Number(row.GIS_Y_COOR) }),
    }));
  }

  async station(id: string): Promise<StationDetail> {
    const { RESULT } = await this.opinet.detailById(id);
    const row = RESULT.OIL[0];
    if (!row)
      throw new NotFoundError('STATION_NOT_FOUND', `Station ${id} not found`);
    return toDetail(row);
  }
}

function toDetail(row: StationDetailRow): StationDetail {
  return {
    id: row.UNI_ID,
    name: row.OS_NM,
    brand: row.POLL_DIV_CD || row.GPOLL_DIV_CD || null,
    address: row.NEW_ADR || row.VAN_ADR,
    tel: row.TEL,
    isLpg: row.LPG_YN !== 'N',
    hasCarWash: row.CAR_WASH_YN === 'Y',
    hasMaintenance: row.MAINT_YN === 'Y',
    hasConvenienceStore: row.CVS_YN === 'Y',
    ...fromKatec({ x: Number(row.GIS_X_COOR), y: Number(row.GIS_Y_COOR) }),
    prices: row.OIL_PRICE.map((p) => ({
      product: p.PRODCD,
      price: Number(p.PRICE),
      reportedAt: `${isoDate(p.TRADE_DT)}T${p.TRADE_TM.slice(0, 2)}:${p.TRADE_TM.slice(2, 4)}:${p.TRADE_TM.slice(4, 6)}`,
    })),
  };
}

/** yyyyMMdd -> yyyy-MM-dd */
function isoDate(yyyymmdd: string): string {
  return `${yyyymmdd.slice(0, 4)}-${yyyymmdd.slice(4, 6)}-${yyyymmdd.slice(6, 8)}`;
}
