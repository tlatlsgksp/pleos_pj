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

/**
 * 오피넷(한국석유공사) 유가 API 연동 지점. Opinet is the planned price source for refuel
 * estimates and fuel-station waypoints (FEATURE_LOGIC L2-4, L4-2); each method is one endpoint
 * from the guides. For the contest the backend answers from StubOpinetClient, which returns the
 * same envelope and fields as the real API, so callers are written as if the API were connected.
 */
export abstract class OpinetClient {
  /** ① 전국 주유소 평균가격 */
  abstract avgAllPrice(): Promise<OpinetEnvelope<AvgAllPriceRow>>;

  /** ② 시도별 주유소 평균가격 */
  abstract avgSidoPrice(params?: {
    sido?: string;
    prodcd?: ProductCode;
  }): Promise<OpinetEnvelope<AvgSidoPriceRow>>;

  /** ④ 최근 7일간 전국 일일 평균가격 */
  abstract avgRecentPrice(params?: {
    prodcd?: ProductCode;
  }): Promise<OpinetEnvelope<AvgRecentPriceRow>>;

  /** ⑧ 지역별 최저가 주유소 TOP20 */
  abstract lowTop10(params: LowTopParams): Promise<OpinetEnvelope<LowTopRow>>;

  /** ⑨ 반경 내 주유소 */
  abstract aroundAll(
    params: AroundAllParams,
  ): Promise<OpinetEnvelope<AroundAllRow>>;

  /** ⑩ 주유소 상세정보 */
  abstract detailById(id: string): Promise<OpinetEnvelope<StationDetailRow>>;

  /** ⑯ 지역코드: no area = 시도 list, a 시도 code = its 시군구 */
  abstract areaCode(area?: string): Promise<OpinetEnvelope<AreaCodeRow>>;
}
