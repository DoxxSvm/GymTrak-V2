import {
  IsNotEmpty,
  IsOptional,
  IsString,
  Matches,
  MinLength,
  ValidateIf,
} from 'class-validator';

/** POST /auth/login: either `phone` (+ optional country_code) or `username` + `password`. */
export class AuthLoginBodyDto {
  @ValidateIf((o: AuthLoginBodyDto) => !o.username && !o.password)
  @IsNotEmpty()
  @IsString()
  @Matches(/^\d{6,15}$/, {
    message: 'phone must contain 6 to 15 digits',
  })
  phone?: string;

  @IsOptional()
  @IsString()
  @Matches(/^\+[1-9]\d{0,4}$/, {
    message: 'country_code must start with + and be valid',
  })
  country_code?: string;

  @ValidateIf((o: AuthLoginBodyDto) => !o.phone)
  @IsNotEmpty()
  @IsString()
  username?: string;

  @ValidateIf((o: AuthLoginBodyDto) => !o.phone)
  @IsNotEmpty()
  @IsString()
  @MinLength(6)
  password?: string;
}
