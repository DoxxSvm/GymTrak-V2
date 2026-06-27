import { IsNotEmpty, IsString, MaxLength } from 'class-validator';

export class SetGymDto {
  @IsString()
  @IsNotEmpty()
  @MaxLength(120)
  gym_name: string;

  @IsString()
  @IsNotEmpty()
  @MaxLength(120)
  owner_name: string;
}
