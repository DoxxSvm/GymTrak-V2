import { ApiProperty } from '@nestjs/swagger';
import {
  ArrayNotEmpty,
  ArrayMaxSize,
  IsArray,
  IsNotEmpty,
  IsString,
} from 'class-validator';

export class AddBroadcastMembersDto {
  @ApiProperty({
    type: [String],
    description:
      '`GymUser.id` values to add; must belong to the same gym as the channel (active membership)',
    maxItems: 200,
  })
  @IsArray()
  @ArrayNotEmpty()
  @ArrayMaxSize(200)
  @IsString({ each: true })
  @IsNotEmpty({ each: true })
  gymUserIds: string[];
}
