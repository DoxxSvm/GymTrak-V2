import { IsIn, IsNotEmpty, IsString } from 'class-validator';

export class SelectRoleDto {
  @IsString()
  @IsNotEmpty()
  @IsIn(['gym_owner', 'trainer'])
  role: 'gym_owner' | 'trainer';
}
