import { AppOnboardingRole } from '@prisma/client';
import { IsEnum, IsNotEmpty } from 'class-validator';

export class ChooseRoleDto {
  @IsNotEmpty()
  @IsEnum(AppOnboardingRole)
  role: AppOnboardingRole;
}
