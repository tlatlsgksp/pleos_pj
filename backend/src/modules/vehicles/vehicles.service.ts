import { Injectable } from '@nestjs/common';
import { NotFoundError } from '../../common/errors/app-error.js';
import { VehiclesRepository } from './vehicles.repository.js';
import type { Vehicle } from './vehicles.types.js';

@Injectable()
export class VehiclesService {
  constructor(private readonly vehicles: VehiclesRepository) {}

  async getById(id: string): Promise<Vehicle> {
    const vehicle = await this.vehicles.findById(id);
    if (!vehicle) {
      throw new NotFoundError('VEHICLE_NOT_FOUND', `Vehicle ${id} not found`);
    }
    return vehicle;
  }
}
