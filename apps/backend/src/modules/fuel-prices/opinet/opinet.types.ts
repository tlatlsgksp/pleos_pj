/**
 * Opinet API rows, field for field as in the 한국석유공사 guides (오피넷 일반 API 2026.10,
 * 프리미엄 API 2026.06). Names stay in Opinet's UPPER_SNAKE so a fixture or a real response can be
 * compared with the guide line by line.
 *
 * The guides show responses only as images. The JSON envelope `{ RESULT: { OIL: [...] } }` and
 * numbers arriving as strings follow Opinet's published JSON output and are an assumption to
 * check when a real key is used; only OpinetEnvelope and the stub depend on it.
 */

/** B034 고급휘발유, B027 보통휘발유, D047 자동차용경유, C004 실내등유, K015 자동차용부탄. */
export type ProductCode = 'B034' | 'B027' | 'D047' | 'C004' | 'K015';

export const PRODUCT_NAMES: Record<ProductCode, string> = {
  B034: '고급휘발유',
  B027: '보통휘발유',
  D047: '자동차용경유',
  C004: '실내등유',
  K015: '자동차용부탄',
};

/** SKE SK에너지, GSC GS칼텍스, HDO HD현대오일뱅크, SOL S-OIL, RTO 알뜰, RTX 알뜰(ex), NHO NH-OIL, SKG SK가스, E1G E1, ETC 자가상표. */
export type BrandCode =
  'SKE' | 'GSC' | 'HDO' | 'SOL' | 'RTO' | 'RTX' | 'NHO' | 'SKG' | 'E1G' | 'ETC';

/** Y/N flags as Opinet sends them. */
export type YN = 'Y' | 'N';

export interface OpinetEnvelope<T> {
  RESULT: { OIL: T[] };
}

/** ① avgAllPrice.do 전국 주유소 평균가격 */
export interface AvgAllPriceRow {
  TRADE_DT: string;
  PRODCD: ProductCode;
  PRODNM: string;
  PRICE: string;
  DIFF: string;
}

/** ② avgSidoPrice.do 시도별 주유소 평균가격 */
export interface AvgSidoPriceRow {
  SIDOCD: string;
  SIDONM: string;
  PRODCD: ProductCode;
  PRICE: string;
  DIFF: string;
}

/** ④ avgRecentPrice.do 최근 7일간 전국 일일 평균가격 */
export interface AvgRecentPriceRow {
  DATE: string;
  PRODCD: ProductCode;
  PRICE: string;
}

/** ⑧ lowTop10.do 지역별 최저가 주유소 */
export interface LowTopRow {
  UNI_ID: string;
  PRICE: string;
  POLL_DIV_CD: BrandCode;
  OS_NM: string;
  VAN_ADR: string;
  NEW_ADR: string;
  GIS_X_COOR: string;
  GIS_Y_COOR: string;
}

/** ⑨ aroundAll.do 반경 내 주유소. DISTANCE is metres. */
export interface AroundAllRow {
  UNI_ID: string;
  POLL_DIV_CD: BrandCode;
  OS_NM: string;
  PRICE: string;
  DISTANCE: string;
  GIS_X_COOR: string;
  GIS_Y_COOR: string;
}

export interface OilPrice {
  PRODCD: ProductCode;
  PRICE: string;
  TRADE_DT: string;
  TRADE_TM: string;
}

/** ⑩ detailById.do 주유소 상세정보. The stub keeps its stations in this shape. */
export interface StationDetailRow {
  UNI_ID: string;
  POLL_DIV_CD: BrandCode | '';
  GPOLL_DIV_CD: BrandCode | '';
  OS_NM: string;
  VAN_ADR: string;
  NEW_ADR: string;
  TEL: string;
  SIGUNCD: string;
  /** N 주유소, Y 자동차충전소, C 겸업 */
  LPG_YN: 'N' | 'Y' | 'C';
  MAINT_YN: YN;
  CAR_WASH_YN: YN;
  CVS_YN: YN;
  KPETRO_YN: YN;
  GOOD_YN: YN;
  GOOD_YN_5: YN;
  GIS_X_COOR: string;
  GIS_Y_COOR: string;
  OIL_PRICE: OilPrice[];
}

/** ⑯ areaCode.do 지역코드 */
export interface AreaCodeRow {
  AREA_CD: string;
  AREA_NM: string;
}

export interface AroundAllParams {
  /** KATEC */
  x: number;
  y: number;
  /** metres, at most 5000 */
  radius: number;
  prodcd: ProductCode;
  /** 1 가격순, 2 거리순 */
  sort: 1 | 2;
}

export interface LowTopParams {
  prodcd: ProductCode;
  /** 시도 2자리 or 시군 4자리; omitted = 전국 */
  area?: string;
  /** 1..20, default 10 */
  cnt?: number;
}
