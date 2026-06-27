import { Transform } from 'class-transformer';
import {
  IsOptional,
  IsString,
  Matches,
  MaxLength,
  ValidateIf,
} from 'class-validator';

/** Gym owner “Edit profile” — personal + gym branding. Phone never updated. */
export class UpdateMeOwnerDto {
  @IsOptional()
  @IsString()
  @MaxLength(120)
  fullName?: string;

  @IsOptional()
  @IsString()
  @MaxLength(2048)
  avatarUrl?: string;

  @IsOptional()
  @IsString()
  @MaxLength(2048)
  profile_image?: string;

  @IsOptional()
  @IsString()
  @MaxLength(200)
  gymName?: string;

  @IsOptional()
  @IsString()
  @MaxLength(500)
  gymAddress?: string;

  /** Omit = no change; `null` or `""` clears GST after transform. */
  @IsOptional()
  @Transform(({ value }) => (value === '' ? null : value))
  @ValidateIf((_, v) => v != null)
  @IsString()
  @MaxLength(15)
  @Matches(/^[0-9A-Z]{15}$/i, {
    message: 'gymGstNumber must be 15 alphanumeric characters',
  })
  gymGstNumber?: string | null;

  @IsOptional()
  @IsString()
  @MaxLength(2048)
  gymLogoUrl?: string;
}
