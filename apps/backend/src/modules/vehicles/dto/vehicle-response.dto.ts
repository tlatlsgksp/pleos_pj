import { ApiProperty } from '@nestjs/swagger';
import type { Vehicle } from '../vehicles.types.js';

/** What the API returns. The VIN hash stays server-side. */
export class VehicleResponseDto {
  @ApiProperty({ format: 'uuid' })
  id: string;

  @ApiProperty({ example: 'hyundai-ioniq5-2025' })
  modelKey: string;

  @ApiProperty({ type: String, format: 'date-time', nullable: true })
  consentAt: string | null;

  @ApiProperty({ format: 'date-time' })
  createdAt: string;

  static from(vehicle: Vehicle): VehicleResponseDto {
    const dto = new VehicleResponseDto();
    dto.id = vehicle.id;
    dto.modelKey = vehicle.modelKey;
    dto.consentAt = vehicle.consentAt?.toISOString() ?? null;
    dto.createdAt = vehicle.createdAt.toISOString();
    return dto;
  }
}
