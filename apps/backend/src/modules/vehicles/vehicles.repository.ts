import { Injectable } from '@nestjs/common';
import type { Vehicle as VehicleRow } from '../../generated/prisma/client.js';
import { PrismaService } from '../../infra/prisma/prisma.service.js';
import type { Vehicle } from './vehicles.types.js';

/** The only file in this module that touches Prisma. */
@Injectable()
export class VehiclesRepository {
  constructor(private readonly prisma: PrismaService) {}

  async findById(id: string): Promise<Vehicle | null> {
    const row = await this.prisma.vehicle.findUnique({ where: { id } });
    return row ? toVehicle(row) : null;
  }
}

function toVehicle(row: VehicleRow): Vehicle {
  return {
    id: row.id,
    vinHash: row.vinHash,
    modelKey: row.modelKey,
    consentAt: row.consentAt,
    createdAt: row.createdAt,
    updatedAt: row.updatedAt,
  };
}
