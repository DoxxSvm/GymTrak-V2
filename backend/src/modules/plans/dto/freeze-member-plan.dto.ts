import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import {
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsString,
  Matches,
  Min,
} from 'class-validator';

/** `POST /plans/freeze` — pause member subscription (extends `endsAt` by freeze duration). */
export class FreezeMemberPlanDto {
  @ApiProperty({ description: 'Gym id (`X-Gym-Id` or body).' })
  @IsString()
  @IsNotEmpty()
  gymId: string;

  @ApiProperty({ description: '`MemberSubscription.id` to freeze.' })
  @IsString()
  @IsNotEmpty()
  member_subscription_id: string;

  @ApiProperty({
    description: 'When freeze starts. `YYYY-MM-DD` (UTC calendar day).',
    example: '2025-10-10',
  })
  @IsString()
  @IsNotEmpty()
  @Matches(/^\d{4}-\d{2}-\d{2}$/)
  freeze_start_date: string;

  @ApiProperty({
    description: 'Freeze length in days (membership end is extended by the same amount).',
    example: 30,
  })
  @Type(() => Number)
  @IsInt()
  @Min(1)
  duration_days: number;

  @ApiPropertyOptional({
    description:
      'Optional freeze fee in minor units (same as `POST /payments` `amount`, stored as `Payment.amountCents`).',
    example: 1500,
  })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  freeze_fee?: number;

  @ApiPropertyOptional({
    description: 'Reason or comments for freezing.',
    example: 'Member traveling abroad',
  })
  @IsOptional()
  @IsString()
  reason?: string;
}

/** `POST /plans/unfreeze` — resume a frozen subscription early. */
export class UnfreezeMemberPlanDto {
  @ApiProperty({ description: 'Gym id (`X-Gym-Id` or body).' })
  @IsString()
  @IsNotEmpty()
  gymId: string;

  @ApiProperty({ description: '`MemberSubscription.id` to unfreeze.' })
  @IsString()
  @IsNotEmpty()
  member_subscription_id: string;
}
