import {
  BadRequestException,
  Controller,
  Get,
  Param,
  Patch,
  Put,
  Query,
} from '@nestjs/common';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { NotificationsService } from './notifications.service';

@Controller('notifications')
export class NotificationsController {
  constructor(private readonly notifications: NotificationsService) {}

  @Get()
  feed(
    @CurrentUser() user: JwtUser,
    @Query('gymId') gymId: string,
    @Query('limit') limit?: string,
    @Query('cursor') cursor?: string,
  ) {
    if (!gymId) {
      throw new BadRequestException('gymId is required');
    }
    return this.notifications.feed(
      user.sub,
      gymId,
      limit ? parseInt(limit, 10) : 20,
      cursor,
    );
  }

  @Patch(':id/read')
  markRead(
    @CurrentUser() user: JwtUser,
    @Param('id') id: string,
    @Query('gymId') gymId: string,
  ) {
    if (!gymId) {
      throw new BadRequestException('gymId is required');
    }
    return this.notifications.markRead(user.sub, gymId, id);
  }

  @Put(':id/read')
  markReadPut(
    @CurrentUser() user: JwtUser,
    @Param('id') id: string,
    @Query('gymId') gymId: string,
  ) {
    if (!gymId) {
      throw new BadRequestException('gymId is required');
    }
    return this.notifications.markRead(user.sub, gymId, id);
  }
}
