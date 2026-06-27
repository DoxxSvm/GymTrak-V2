import { IsIn } from 'class-validator';

/** Platform block/unblock maps to gym lifecycle (not ARCHIVED). */
export class SetPlatformGymStatusDto {
  @IsIn(['ACTIVE', 'SUSPENDED'])
  status: 'ACTIVE' | 'SUSPENDED';
}
