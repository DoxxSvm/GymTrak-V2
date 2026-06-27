import {
  BadRequestException,
  Body,
  Controller,
  Get,
  Param,
  Patch,
  Post,
  Put,
  Query,
  UsePipes,
  ValidationPipe,
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiBody,
  ApiForbiddenResponse,
  ApiNotFoundResponse,
  ApiOkResponse,
  ApiOperation,
  ApiQuery,
  ApiTags,
} from '@nestjs/swagger';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { RegisterDeviceTokenDto } from './dto/register-device-token.dto';
import { SendTestNotificationDto } from './dto/send-test-notification.dto';
import { NotificationsService } from './notifications.service';

const DEVICE_TOKEN_PIPE = new ValidationPipe({
  transform: true,
  whitelist: true,
  forbidNonWhitelisted: false,
});

const SEND_TEST_PIPE = new ValidationPipe({
  transform: true,
  whitelist: true,
  forbidNonWhitelisted: true,
});

@ApiTags('Notifications')
@ApiBearerAuth()
@Controller('notifications')
export class NotificationsController {
  constructor(private readonly notifications: NotificationsService) {}

  /** Register FCM token so gym-owner notifications also arrive as push when WebSocket is offline. */
  @Put('device-token')
  @UsePipes(DEVICE_TOKEN_PIPE)
  @ApiOperation({
    summary: 'Register FCM device token',
    description:
      'Save the Firebase Cloud Messaging registration token for the current user. Send empty `token` to unregister.',
  })
  @ApiBody({ type: RegisterDeviceTokenDto })
  registerDeviceToken(
    @CurrentUser() user: JwtUser,
    @Body() body: RegisterDeviceTokenDto,
  ) {
    return this.notifications.registerDeviceToken(user.sub, body.token);
  }

  @Get()
  @ApiOperation({
    summary: 'Notification feed (cursor paginated)',
    description:
      'Lists notifications for the authenticated user (`User.id` = JWT `sub`, `recipientUserId`). Optional `gymId` narrows to one gym.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description: 'Optional gym filter',
  })
  @ApiQuery({
    name: 'limit',
    required: false,
    description: 'Page size (1–50, default 20)',
  })
  @ApiQuery({
    name: 'cursor',
    required: false,
    description: 'Notification id from previous `nextCursor`',
  })
  @ApiOkResponse({
    description:
      'Feed page (`items` + legacy `data` + `nextCursor`). Salary notifications include `screen`: `trainer-salary` or `staff-salary` (also in `metadata.deepLink.screen`).',
    schema: {
      type: 'object',
      properties: {
        items: { type: 'array', items: { type: 'object' } },
        data: { type: 'array', items: { type: 'object' } },
        nextCursor: { type: 'string', nullable: true },
      },
    },
  })
  feed(
    @CurrentUser() user: JwtUser,
    @Query('gymId') gymId?: string,
    @Query('limit') limit?: string,
    @Query('cursor') cursor?: string,
  ) {
    const parsedLimit = limit ? parseInt(limit, 10) : 20;
    if (Number.isNaN(parsedLimit)) {
      throw new BadRequestException('limit must be a number');
    }
    return this.notifications.feed(
      user.sub,
      parsedLimit,
      cursor,
      gymId?.trim() || undefined,
    );
  }

  @Post('send-test')
  @UsePipes(SEND_TEST_PIPE)
  @ApiOperation({
    summary: 'Send test notification (WebSocket + FCM)',
    description:
      'Targets a user by `phone`. Normal users may only use their own phone; SUPER_ADMIN may target any user. Does not persist a notification row.',
  })
  @ApiBody({ type: SendTestNotificationDto })
  @ApiOkResponse({
    description:
      'Synthetic WebSocket payload emitted; FCM attempted when token + Firebase are configured',
    schema: {
      type: 'object',
      required: ['ok', 'websocket', 'fcm'],
      properties: {
        ok: { type: 'boolean', enum: [true] },
        websocket: { type: 'boolean', enum: [true] },
        fcm: {
          type: 'object',
          properties: {
            attempted: { type: 'boolean' },
            delivered: { type: 'boolean' },
            reason: {
              type: 'string',
              enum: ['no_device_token', 'fcm_not_configured'],
            },
            error: {
              type: 'string',
              enum: ['send_failed', 'invalid_token_cleared'],
            },
          },
        },
      },
    },
  })
  @ApiForbiddenResponse({
    description:
      'Caller may only send a test to their own phone (unless SUPER_ADMIN)',
  })
  @ApiNotFoundResponse({ description: 'User not found' })
  sendTest(
    @CurrentUser() user: JwtUser,
    @Body() body: SendTestNotificationDto,
  ) {
    return this.notifications.sendTestNotification(user, body.phone);
  }

  @Patch(':id/read')
  @ApiOperation({
    summary: 'Mark notification read',
    description:
      'Sets `readAt` when the notification belongs to the authenticated user (`recipientUserId`). Optional `gymId` scopes the lookup.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description: 'Optional gym filter',
  })
  @ApiOkResponse({
    description: 'Marked read (no-op if id not found for this user)',
    schema: {
      type: 'object',
      required: ['ok'],
      properties: { ok: { type: 'boolean', enum: [true] } },
    },
  })
  markRead(
    @CurrentUser() user: JwtUser,
    @Param('id') id: string,
    @Query('gymId') gymId?: string,
  ) {
    return this.notifications.markRead(
      user.sub,
      id,
      gymId?.trim() || undefined,
    );
  }

  @Put(':id/read')
  @ApiOperation({
    summary: 'Mark notification read (PUT alias)',
    description: 'Same as PATCH `/:id/read`.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description: 'Optional gym filter',
  })
  @ApiOkResponse({
    description: 'Marked read',
    schema: {
      type: 'object',
      required: ['ok'],
      properties: { ok: { type: 'boolean', enum: [true] } },
    },
  })
  markReadPut(
    @CurrentUser() user: JwtUser,
    @Param('id') id: string,
    @Query('gymId') gymId?: string,
  ) {
    return this.notifications.markRead(
      user.sub,
      id,
      gymId?.trim() || undefined,
    );
  }
}
