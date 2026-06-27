import { Transform } from 'class-transformer';
import { IsOptional, IsString, Matches, MaxLength } from 'class-validator';

function trimGender(v: unknown): string | undefined {
  if (v == null || typeof v !== 'string') return undefined;
  const s = v.trim().toLowerCase();
  if (s === 'male' || s === 'female' || s === 'other') return s;
  return undefined;
}

/** Member “Edit profile” — phone is never accepted or changed. */
export class UpdateMeMemberDto {
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
  @MaxLength(120)
  fullName?: string;

  @IsOptional()
  @IsString()
  @MaxLength(120)
  name?: string;

  @IsOptional()
  @Transform(({ value }) => trimGender(value))
  @IsString()
  @Matches(/^(male|female|other)$/)
  gender?: string;

  @IsOptional()
  @IsString()
  @MaxLength(40)
  dateOfBirth?: string;
}
