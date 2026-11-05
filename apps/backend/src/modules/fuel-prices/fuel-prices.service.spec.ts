import { NotFoundError } from '../../common/errors/app-error.js';
import { FuelPricesService } from './fuel-prices.service.js';
import { fromKatec, toKatec } from './opinet/katec.js';
import { StubOpinetClient } from './opinet/stub-opinet.client.js';

// Runs the service on the real stub, the same pairing the module binds.
const service = new FuelPricesService(new StubOpinetClient());
const seoulStation = { lat: 37.5547, lng: 126.9707 };

describe('KATEC conversion', () => {
  it('places the Opinet guide example in Gangnam and converts back', () => {
    const point = fromKatec({ x: 314681.8, y: 544837 });
    expect(point.lat).toBeCloseTo(37.5018, 3);
    expect(point.lng).toBeCloseTo(127.0329, 3);
    const back = toKatec(point);
    expect(back.x).toBeCloseTo(314681.8, 1);
    expect(back.y).toBeCloseTo(544837, 1);
  });
});

describe('FuelPricesService on the Opinet stub', () => {
  it('returns national averages as numbers with ISO dates', async () => {
    const averages = await service.nationalAverages();
    const gasoline = averages.find((a) => a.product === 'B027')!;
    expect(gasoline.productName).toBe('보통휘발유');
    expect(gasoline.price).toBeGreaterThan(1000);
    expect(gasoline.date).toBe('2026-11-05');
  });

  it('finds stations within the radius, cheapest first', async () => {
    const stations = await service.nearby(
      seoulStation.lat,
      seoulStation.lng,
      3000,
      'B027',
      'price',
    );
    expect(stations.length).toBeGreaterThanOrEqual(3);
    expect(stations.every((s) => s.distanceM <= 3000)).toBe(true);
    const prices = stations.map((s) => s.price);
    expect(prices).toEqual([...prices].sort((a, b) => a - b));
    // Coordinates come back in WGS84 near the query point.
    expect(stations[0].lat).toBeCloseTo(seoulStation.lat, 1);
  });

  it('sorts by distance on request and leaves out stations that do not sell the product', async () => {
    const byDistance = await service.nearby(
      seoulStation.lat,
      seoulStation.lng,
      5000,
      'B027',
      'distance',
    );
    const distances = byDistance.map((s) => s.distanceM);
    expect(distances).toEqual([...distances].sort((a, b) => a - b));

    const lpg = await service.nearby(37.4979, 127.0276, 2000, 'K015', 'price');
    expect(lpg.map((s) => s.name)).toEqual(['시연 강남LPG충전소']);
  });

  it('caps the radius at 5 km like Opinet', async () => {
    const far = await service.nearby(
      seoulStation.lat,
      seoulStation.lng,
      50_000,
      'B027',
      'price',
    );
    expect(far.every((s) => s.distanceM <= 5000)).toBe(true);
    expect(far.some((s) => s.name.includes('판교'))).toBe(false);
  });

  it('returns station detail with prices, or NotFoundError', async () => {
    const detail = await service.station('A9900001');
    expect(detail.name).toBe('시연 서울역주유소');
    expect(detail.prices.find((p) => p.product === 'B027')?.reportedAt).toBe(
      '2026-11-05T16:00:00',
    );
    await expect(service.station('A0000000')).rejects.toBeInstanceOf(
      NotFoundError,
    );
  });
});
