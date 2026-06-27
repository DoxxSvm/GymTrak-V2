import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { ExpenseCategory } from '@prisma/client';

/** Unified expense JSON for list / get / create / update. */
export class ExpenseResponseSwagger {
  @ApiProperty()
  id: string;

  @ApiProperty()
  gymId: string;

  @ApiProperty()
  bill_name: string;

  @ApiProperty({ enum: ExpenseCategory, enumName: 'ExpenseCategory' })
  category: ExpenseCategory;

  @ApiProperty({ example: '2026-04-13' })
  date: string;

  @ApiPropertyOptional({ nullable: true })
  trainer_id: string | null;

  @ApiPropertyOptional({
    nullable: true,
    enum: ['cash', 'upi', 'card'],
  })
  payment_mode: string | null;

  @ApiPropertyOptional({ nullable: true })
  gst: number | null;

  @ApiProperty({ description: 'Amount in major currency units' })
  amount: number;
}

export class ExpenseListResponseSwagger {
  @ApiProperty()
  total: number;

  @ApiProperty()
  limit: number;

  @ApiProperty()
  offset: number;

  @ApiProperty({ type: [ExpenseResponseSwagger] })
  items: ExpenseResponseSwagger[];
}

export class ExpenseDeleteOkSwagger {
  @ApiProperty({ example: true })
  ok: boolean;
}
