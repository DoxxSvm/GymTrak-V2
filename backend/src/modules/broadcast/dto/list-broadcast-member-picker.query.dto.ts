import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsNotEmpty, IsOptional, IsString, MaxLength } from 'class-validator';

export class ListBroadcastMemberPickerQueryDto {
  @ApiProperty({ description: 'Target gym id' })
  @IsString()
  @IsNotEmpty()
  gymId: string;

  @ApiPropertyOptional({
    maxLength: 200,
    description:
      'Filter by substring on user fullName, phone, or username (name/username case-insensitive)',
  })
  @IsOptional()
  @IsString()
  @MaxLength(200)
  search?: string;
}
