import { IsUUID } from 'class-validator';

export class VehicleIdParams {
  @IsUUID()
  id: string;
}
