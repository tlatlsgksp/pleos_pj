import { ApiProperty } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import {
  IsIn,
  IsInt,
  IsLatitude,
  IsLongitude,
  IsOptional,
  IsString,
  Length,
  Max,
  Min,
} from 'class-validator';
import type {
  NearbyStation,
  ProductAverage,
  StationDetail,
  StationPrice,
} from '../fuel-prices.types.js';
import { MAX_RADIUS_M } from '../fuel-prices.service.js';
import type { BrandCode, ProductCode } from '../opinet/opinet.types.js';

const PRODUCTS: ProductCode[] = ['B034', 'B027', 'D047', 'C004', 'K015'];

export class NearbyQuery {
  @ApiProperty({ example: 37.5547 })
  @Type(() => Number)
  @IsLatitude()
  lat: number;

  @ApiProperty({ example: 126.9707 })
  @Type(() => Number)
  @IsLongitude()
  lng: number;

  @ApiProperty({
    required: false,
    default: 3000,
    maximum: MAX_RADIUS_M,
    description: 'metres',
  })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(100)
  @Max(MAX_RADIUS_M)
  radius: number = 3000;

  @ApiProperty({
    required: false,
    enum: PRODUCTS,
    default: 'B027',
    description: 'B027 보통휘발유, D047 경유, B034 고급휘발유, K015 LPG',
  })
  @IsOptional()
  @IsIn(PRODUCTS)
  product: ProductCode = 'B027';

  @ApiProperty({
    required: false,
    enum: ['price', 'distance'],
    default: 'price',
  })
  @IsOptional()
  @IsIn(['price', 'distance'])
  sort: 'price' | 'distance' = 'price';
}

export class StationIdParams {
  @IsString()
  @Length(8, 10)
  id: string;
}

export class ProductAverageDto implements ProductAverage {
  @ApiProperty({ enum: PRODUCTS }) product: ProductCode;
  @ApiProperty({ example: '보통휘발유' }) productName: string;
  @ApiProperty({ example: 1668.42 }) price: number;
  @ApiProperty({ example: -0.87 }) diff: number;
  @ApiProperty({ example: '2026-11-05' }) date: string;
}

export class NearbyStationDto implements NearbyStation {
  @ApiProperty({ example: 'A9900002' }) id: string;
  @ApiProperty() name: string;
  @ApiProperty({ example: 'RTO' }) brand: BrandCode;
  @ApiProperty({ example: 1704 }) price: number;
  @ApiProperty({ example: 512 }) distanceM: number;
  @ApiProperty() lat: number;
  @ApiProperty() lng: number;
}

export class StationPriceDto implements StationPrice {
  @ApiProperty({ enum: PRODUCTS }) product: ProductCode;
  @ApiProperty() price: number;
  @ApiProperty({ example: '2026-11-05T16:00:00' }) reportedAt: string;
}

export class StationDetailDto implements StationDetail {
  @ApiProperty() id: string;
  @ApiProperty() name: string;
  @ApiProperty({ type: String, nullable: true }) brand: BrandCode | null;
  @ApiProperty() address: string;
  @ApiProperty() tel: string;
  @ApiProperty() isLpg: boolean;
  @ApiProperty() hasCarWash: boolean;
  @ApiProperty() hasMaintenance: boolean;
  @ApiProperty() hasConvenienceStore: boolean;
  @ApiProperty() lat: number;
  @ApiProperty() lng: number;
  @ApiProperty({ type: [StationPriceDto] }) prices: StationPriceDto[];
}
