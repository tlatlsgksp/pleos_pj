import { Module } from '@nestjs/common';
import { FuelPricesController } from './fuel-prices.controller.js';
import { FuelPricesService } from './fuel-prices.service.js';
import { OpinetClient } from './opinet/opinet.client.js';
import { StubOpinetClient } from './opinet/stub-opinet.client.js';

@Module({
  controllers: [FuelPricesController],
  providers: [
    FuelPricesService,
    // The Opinet integration point. Bound to the stub: the contest build makes no external calls.
    { provide: OpinetClient, useClass: StubOpinetClient },
  ],
  exports: [FuelPricesService],
})
export class FuelPricesModule {}
