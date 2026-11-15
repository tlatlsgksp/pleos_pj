// Writes the Opinet stub fixtures under src/modules/fuel-prices/opinet/fixtures/.
// Usage: node scripts/generate-opinet-fixtures.mjs
//
// Everything here is made up for the demo: fictional "시연" stations near the demo route
// (Seoul -> Daejeon), plausible prices, and illustrative 시군 codes. Field names and value
// formats follow the Opinet guides so the stub answers exactly like the real API would.
// Deterministic: the same input always produces the same files.
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import proj4 from 'proj4';

const OUT = join(dirname(fileURLToPath(import.meta.url)), '../src/modules/fuel-prices/opinet/fixtures');
const KATEC =
  '+proj=tmerc +lat_0=38 +lon_0=128 +k=0.9999 +x_0=400000 +y_0=600000 ' +
  '+ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43';

const TRADE_DT = '20261105';
const TRADE_TM = '160000';

/** Product averages on TRADE_DT, won per litre. */
const NATIONAL = { B034: 1935.77, B027: 1668.42, D047: 1571.03, C004: 1312.5, K015: 1012.34 };
const NAMES = { B034: '고급휘발유', B027: '보통휘발유', D047: '자동차용경유', C004: '실내등유', K015: '자동차용부탄' };
const DIFF = { B034: -0.41, B027: -0.87, D047: -1.12, C004: 0.0, K015: 0.35 };

/** 시도 codes as Opinet's areaCode lists them, with a price offset from the national average. */
const SIDO = [
  ['01', '서울', 78], ['02', '경기', 6], ['03', '강원', 12], ['04', '충북', 3], ['05', '충남', 4],
  ['06', '전북', -4], ['07', '전남', 9], ['08', '경북', -2], ['09', '경남', -3], ['10', '부산', 1],
  ['11', '제주', 64], ['14', '대구', -9], ['15', '인천', 2], ['16', '광주', -1], ['17', '대전', 0],
  ['18', '울산', -5], ['19', '세종', 7],
];

/** Illustrative 시군 codes for the areas the stations sit in. */
const SIGUN = [
  ['0102', '중구'], ['0116', '강남구'], ['0204', '수원시'], ['0209', '성남시'], ['0502', '천안시'], ['1702', '대전 중구'],
];

/** [name, brand, lat, lng, sigun, address, lpg, price offset from the 시도 average] */
const STATIONS = [
  ['시연 서울역주유소', 'SKE', 37.5531, 126.9726, '0102', '서울특별시 중구 시연로 11', 'N', 35],
  ['시연 남대문셀프주유소', 'RTO', 37.5589, 126.9779, '0102', '서울특별시 중구 시연로 27', 'N', -42],
  ['시연 회현주유소', 'GSC', 37.5571, 126.9811, '0102', '서울특별시 중구 시연로 39', 'N', 12],
  ['시연 강남대로주유소', 'HDO', 37.4991, 127.0287, '0116', '서울특별시 강남구 시연대로 120', 'N', 55],
  ['시연 역삼셀프주유소', 'SOL', 37.5009, 127.0352, '0116', '서울특별시 강남구 시연대로 152', 'N', -18],
  ['시연 테헤란주유소', 'SKE', 37.5048, 127.0410, '0116', '서울특별시 강남구 시연로 205', 'N', 21],
  ['시연 강남LPG충전소', 'E1G', 37.4952, 127.0331, '0116', '서울특별시 강남구 시연로 88', 'Y', 0],
  ['시연 판교주유소', 'GSC', 37.3952, 127.1098, '0209', '경기도 성남시 분당구 시연로 7', 'N', 8],
  ['시연 판교알뜰주유소', 'RTX', 37.3921, 127.1150, '0209', '경기도 성남시 분당구 시연로 19', 'N', -30],
  ['시연 수원IC주유소', 'NHO', 37.2648, 127.0018, '0204', '경기도 수원시 팔달구 시연로 3', 'N', -11],
  ['시연 수원셀프주유소', 'SOL', 37.2711, 127.0063, '0204', '경기도 수원시 팔달구 시연로 41', 'N', 4],
  ['시연 천안주유소', 'HDO', 36.8152, 127.1141, '0502', '충청남도 천안시 동남구 시연로 15', 'N', -6],
  ['시연 천안알뜰주유소', 'RTO', 36.8189, 127.1187, '0502', '충청남도 천안시 동남구 시연로 33', 'N', -27],
  ['시연 대전역주유소', 'SKE', 36.3322, 127.4342, '1702', '대전광역시 중구 시연로 2', 'N', 10],
  ['시연 대전셀프주유소', 'GSC', 36.3271, 127.4270, '1702', '대전광역시 중구 시연로 58', 'N', -15],
];

const money = (n) => n.toFixed(0);
const sidoOffset = (sigun) => SIDO.find(([code]) => code === sigun.slice(0, 2))[2];

function write(name, rows) {
  writeFileSync(join(OUT, name), `${JSON.stringify(rows, null, 2)}\n`);
}

mkdirSync(OUT, { recursive: true });

write('avgAllPrice.json', Object.keys(NATIONAL).map((p) => ({
  TRADE_DT, PRODCD: p, PRODNM: NAMES[p], PRICE: NATIONAL[p].toFixed(2), DIFF: DIFF[p].toFixed(2),
})));

write('avgSidoPrice.json', SIDO.flatMap(([code, name, offset]) => Object.keys(NATIONAL).map((p) => ({
  SIDOCD: code, SIDONM: name, PRODCD: p, PRICE: (NATIONAL[p] + offset).toFixed(2), DIFF: DIFF[p].toFixed(2),
}))));

// Seven confirmed daily averages before TRADE_DT, drifting down slowly towards today.
write('avgRecentPrice.json', Array.from({ length: 7 }, (_, i) => 7 - i).flatMap((daysAgo) => {
  const d = new Date(Date.UTC(2026, 10, 5 - daysAgo));
  const date = d.toISOString().slice(0, 10).replaceAll('-', '');
  return Object.keys(NATIONAL).map((p) => ({ DATE: date, PRODCD: p, PRICE: (NATIONAL[p] + daysAgo * 0.9).toFixed(2) }));
}));

write('areaCode.json', [
  ...SIDO.map(([code, name]) => ({ AREA_CD: code, AREA_NM: name })),
  ...SIGUN.map(([code, name]) => ({ AREA_CD: code, AREA_NM: name })),
]);

write('stations.json', STATIONS.map(([name, brand, lat, lng, sigun, address, lpg, offset], i) => {
  const [x, y] = proj4('EPSG:4326', KATEC, [lng, lat]);
  const base = sidoOffset(sigun) + offset;
  const products = lpg === 'Y' ? ['K015'] : ['B027', 'D047', ...(i % 3 === 0 ? ['B034'] : [])];
  return {
    UNI_ID: `A99${String(i + 1).padStart(5, '0')}`,
    POLL_DIV_CD: lpg === 'Y' ? '' : brand,
    GPOLL_DIV_CD: lpg === 'Y' ? brand : '',
    OS_NM: name,
    VAN_ADR: address.replace('시연로', '시연동'),
    NEW_ADR: address,
    TEL: `02-0000-${String(1000 + i).padStart(4, '0')}`,
    SIGUNCD: sigun,
    LPG_YN: lpg,
    MAINT_YN: i % 4 === 0 ? 'Y' : 'N',
    CAR_WASH_YN: i % 2 === 0 ? 'Y' : 'N',
    CVS_YN: i % 3 === 0 ? 'Y' : 'N',
    KPETRO_YN: 'Y',
    GOOD_YN: offset < -20 ? 'Y' : 'N',
    GOOD_YN_5: 'N',
    GIS_X_COOR: x.toFixed(1),
    GIS_Y_COOR: y.toFixed(1),
    OIL_PRICE: products.map((p) => ({ PRODCD: p, PRICE: money(NATIONAL[p] + base), TRADE_DT, TRADE_TM })),
  };
}));

console.log(`wrote fixtures to ${OUT}`);
