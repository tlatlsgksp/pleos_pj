import { Injectable } from '@nestjs/common';
import areaCodes from './fixtures/areaCode.json' with { type: 'json' };
import avgAll from './fixtures/avgAllPrice.json' with { type: 'json' };
import avgRecent from './fixtures/avgRecentPrice.json' with { type: 'json' };
import avgSido from './fixtures/avgSidoPrice.json' with { type: 'json' };
import stations from './fixtures/stations.json' with { type: 'json' };
import { katecDistanceMetres } from './katec.js';
import { OpinetClient } from './opinet.client.js';
import type {
  AreaCodeRow,
  AroundAllParams,
  AroundAllRow,
  AvgAllPriceRow,
  AvgRecentPriceRow,
  AvgSidoPriceRow,
  LowTopParams,
  LowTopRow,
  OpinetEnvelope,
  ProductCode,
  StationDetailRow,
} from './opinet.types.js';

const MAX_RADIUS_M = 5000;

function envelope<T>(rows: T[]): OpinetEnvelope<T> {
  return { RESULT: { OIL: rows } };
}

/**
 * Answers every Opinet call from the JSON fixtures, applying the same parameters the real API
 * takes (radius, sort, area, cnt). No network. The stations are fictional ("시연" names, A99
 * ids) placed near the demo route; prices are plausible, not real.
 */
@Injectable()
export class StubOpinetClient extends OpinetClient {
  private readonly stations = stations as StationDetailRow[];

  avgAllPrice(): Promise<OpinetEnvelope<AvgAllPriceRow>> {
    return Promise.resolve(envelope(avgAll as AvgAllPriceRow[]));
  }

  avgSidoPrice(params: { sido?: string; prodcd?: ProductCode } = {}) {
    const rows = (avgSido as AvgSidoPriceRow[]).filter(
      (row) =>
        (!params.sido || row.SIDOCD === params.sido) &&
        (!params.prodcd || row.PRODCD === params.prodcd),
    );
    return Promise.resolve(envelope(rows));
  }

  avgRecentPrice(params: { prodcd?: ProductCode } = {}) {
    const rows = (avgRecent as AvgRecentPriceRow[]).filter(
      (row) => !params.prodcd || row.PRODCD === params.prodcd,
    );
    return Promise.resolve(envelope(rows));
  }

  lowTop10(params: LowTopParams): Promise<OpinetEnvelope<LowTopRow>> {
    const cnt = Math.min(Math.max(params.cnt ?? 10, 1), 20);
    const rows = this.stations
      .filter((s) => !params.area || s.SIGUNCD.startsWith(params.area))
      .flatMap((s) => {
        const price = priceOf(s, params.prodcd);
        return price === undefined
          ? []
          : [
              {
                UNI_ID: s.UNI_ID,
                PRICE: price,
                POLL_DIV_CD: (s.POLL_DIV_CD ||
                  s.GPOLL_DIV_CD) as LowTopRow['POLL_DIV_CD'],
                OS_NM: s.OS_NM,
                VAN_ADR: s.VAN_ADR,
                NEW_ADR: s.NEW_ADR,
                GIS_X_COOR: s.GIS_X_COOR,
                GIS_Y_COOR: s.GIS_Y_COOR,
              },
            ];
      })
      .sort((a, b) => Number(a.PRICE) - Number(b.PRICE))
      .slice(0, cnt);
    return Promise.resolve(envelope(rows));
  }

  aroundAll(params: AroundAllParams): Promise<OpinetEnvelope<AroundAllRow>> {
    const radius = Math.min(params.radius, MAX_RADIUS_M);
    const rows = this.stations
      .flatMap((s) => {
        const price = priceOf(s, params.prodcd);
        if (price === undefined) return [];
        const distance = katecDistanceMetres(
          { x: params.x, y: params.y },
          { x: Number(s.GIS_X_COOR), y: Number(s.GIS_Y_COOR) },
        );
        if (distance > radius) return [];
        return [
          {
            UNI_ID: s.UNI_ID,
            POLL_DIV_CD: (s.POLL_DIV_CD ||
              s.GPOLL_DIV_CD) as AroundAllRow['POLL_DIV_CD'],
            OS_NM: s.OS_NM,
            PRICE: price,
            DISTANCE: distance.toFixed(1),
            GIS_X_COOR: s.GIS_X_COOR,
            GIS_Y_COOR: s.GIS_Y_COOR,
          },
        ];
      })
      .sort((a, b) =>
        params.sort === 1
          ? Number(a.PRICE) - Number(b.PRICE)
          : Number(a.DISTANCE) - Number(b.DISTANCE),
      );
    return Promise.resolve(envelope(rows));
  }

  detailById(id: string): Promise<OpinetEnvelope<StationDetailRow>> {
    return Promise.resolve(
      envelope(this.stations.filter((s) => s.UNI_ID === id)),
    );
  }

  areaCode(area?: string): Promise<OpinetEnvelope<AreaCodeRow>> {
    const rows = (areaCodes as AreaCodeRow[]).filter((row) =>
      area
        ? row.AREA_CD.length === 4 && row.AREA_CD.startsWith(area)
        : row.AREA_CD.length === 2,
    );
    return Promise.resolve(envelope(rows));
  }
}

function priceOf(
  station: StationDetailRow,
  prodcd: ProductCode,
): string | undefined {
  return station.OIL_PRICE.find((p) => p.PRODCD === prodcd)?.PRICE;
}
