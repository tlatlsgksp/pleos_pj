import { NotFoundError } from '../../common/errors/app-error.js';
import type { VehiclesRepository } from './vehicles.repository.js';
import { VehiclesService } from './vehicles.service.js';
import type { Vehicle } from './vehicles.types.js';

const vehicle: Vehicle = {
  id: '6f1e2c3a-0000-4000-8000-000000000001',
  vinHash: 'abc',
  modelKey: 'hyundai-ioniq5-2025',
  consentAt: null,
  createdAt: new Date('2026-01-01T00:00:00Z'),
  updatedAt: new Date('2026-01-01T00:00:00Z'),
};

describe('VehiclesService', () => {
  const repository = { findById: vi.fn() };
  const service = new VehiclesService(
    repository as unknown as VehiclesRepository,
  );

  beforeEach(() => repository.findById.mockReset());

  it('returns the vehicle when it exists', async () => {
    repository.findById.mockResolvedValue(vehicle);
    await expect(service.getById(vehicle.id)).resolves.toEqual(vehicle);
  });

  it('throws NotFoundError when it does not', async () => {
    repository.findById.mockResolvedValue(null);
    await expect(service.getById(vehicle.id)).rejects.toBeInstanceOf(
      NotFoundError,
    );
  });
});
