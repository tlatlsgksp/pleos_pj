import { Controller, Get, Param, Query } from '@nestjs/common';
import { ApiOkResponse, ApiTags } from '@nestjs/swagger';
import {
  NearbyQuery,
  NearbyStationDto,
  ProductAverageDto,
  StationDetailDto,
  StationIdParams,
} from './dto/fuel-prices.dto.js';
import { FuelPricesService } from './fuel-prices.service.js';

/** Fuel prices and stations from Opinet, shaped for the car app. */
@ApiTags('fuel-prices')
@Controller({ path: 'fuel-prices', version: '1' })
export class FuelPricesController {
  constructor(private readonly prices: FuelPricesService) {}

  @Get('averages')
  @ApiOkResponse({ type: [ProductAverageDto] })
  averages(): Promise<ProductAverageDto[]> {
    return this.prices.nationalAverages();
  }

  @Get('stations/nearby')
  @ApiOkResponse({ type: [NearbyStationDto] })
  nearby(@Query() q: NearbyQuery): Promise<NearbyStationDto[]> {
    return this.prices.nearby(q.lat, q.lng, q.radius, q.product, q.sort);
  }

  @Get('stations/:id')
  @ApiOkResponse({ type: StationDetailDto })
  station(@Param() params: StationIdParams): Promise<StationDetailDto> {
    return this.prices.station(params.id);
  }
}
