import { Module } from '@nestjs/common';
import { VehiclesController } from './vehicles.controller.js';
import { VehiclesRepository } from './vehicles.repository.js';
import { VehiclesService } from './vehicles.service.js';

@Module({
  controllers: [VehiclesController],
  providers: [VehiclesService, VehiclesRepository],
  // Only the service crosses the module boundary.
  exports: [VehiclesService],
})
export class VehiclesModule {}
