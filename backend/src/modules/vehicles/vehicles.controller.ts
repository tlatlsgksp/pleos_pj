import { Controller, Get, Param } from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiOkResponse,
  ApiParam,
  ApiTags,
} from '@nestjs/swagger';
import { VehicleIdParams } from './dto/vehicle-id.params.js';
import { VehicleResponseDto } from './dto/vehicle-response.dto.js';
import { VehiclesService } from './vehicles.service.js';

@ApiTags('vehicles')
@ApiBearerAuth()
@Controller({ path: 'vehicles', version: '1' })
export class VehiclesController {
  constructor(private readonly vehicles: VehiclesService) {}

  @Get(':id')
  @ApiParam({ name: 'id', format: 'uuid' })
  @ApiOkResponse({ type: VehicleResponseDto })
  async getById(@Param() params: VehicleIdParams): Promise<VehicleResponseDto> {
    return VehicleResponseDto.from(await this.vehicles.getById(params.id));
  }
}
