import proj4 from 'proj4';

/**
 * KATEC (TM128, Bessel), the coordinate system every Opinet location uses (GIS_X_COOR, GIS_Y_COOR,
 * aroundAll x/y). Checked against the guide's own example: x=314681.8, y=544837 lands at
 * 37.5018, 127.0329 (Gangnam, Seoul) and converts back exactly.
 */
const KATEC =
  '+proj=tmerc +lat_0=38 +lon_0=128 +k=0.9999 +x_0=400000 +y_0=600000 ' +
  '+ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43';
const WGS84 = 'EPSG:4326';

export interface LatLng {
  lat: number;
  lng: number;
}

export interface Katec {
  x: number;
  y: number;
}

export function toKatec({ lat, lng }: LatLng): Katec {
  const [x, y] = proj4(WGS84, KATEC, [lng, lat]);
  return { x, y };
}

export function fromKatec({ x, y }: Katec): LatLng {
  const [lng, lat] = proj4(KATEC, WGS84, [x, y]);
  return { lat, lng };
}

/** KATEC is a metric projection, so plain Euclidean distance is accurate within a few km. */
export function katecDistanceMetres(a: Katec, b: Katec): number {
  return Math.hypot(a.x - b.x, a.y - b.y);
}
