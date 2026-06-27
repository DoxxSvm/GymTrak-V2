import { IsIn, IsNotEmpty, IsString } from 'class-validator';

/** Unified role pick: owner-app strings + Prisma `AppOnboardingRole` for member path. */
export class SelectRoleDto {
  @IsString()
  @IsNotEmpty()
  @IsIn(['gym_owner', 'trainer', 'owner', 'member'])
  role: 'gym_owner' | 'trainer' | 'owner' | 'member';
}
