import { Body, Controller, Post, Query } from '@nestjs/common';
import {
  ApiBadRequestResponse,
  ApiBearerAuth,
  ApiBody,
  ApiForbiddenResponse,
  ApiNotFoundResponse,
  ApiOkResponse,
  ApiOperation,
  ApiTags,
} from '@nestjs/swagger';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { TestWhatsAppMessageDto } from './dto/test-whatsapp-message.dto';
import { WhatsAppTestService } from './whatsapp-test.service';

/**
 * Synchronous WhatsApp test sends for gym managers (bypasses BullMQ queue).
 * Requires `X-Gym-Id` or `gymId` query param.
 */
@ApiTags('WhatsApp')
@ApiBearerAuth()
@Controller('whatsapp/test')
export class WhatsAppTestController {
  constructor(private readonly whatsappTest: WhatsAppTestService) {}

  @Post('send')
  @ApiOperation({
    summary: 'Send test WhatsApp message (synchronous)',
    description:
      'Sends a Meta WhatsApp template or plain text **immediately** (no BullMQ queue). Caller must be able to manage the gym (`gymId` query or `X-Gym-Id`). Provide **`phone`** or **`member_id`** (`GymUser.id`). When `WHATSAPP_SKIP_SEND=true`, logs the payload only (`sent: false`, `skipped: true`).',
  })
  @ApiBody({ type: TestWhatsAppMessageDto })
  @ApiOkResponse({
    description: 'Message dispatched or skipped (dry-run)',
    schema: {
      type: 'object',
      required: ['sent', 'skipped', 'type', 'to', 'provider'],
      properties: {
        sent: {
          type: 'boolean',
          description: 'False when WHATSAPP_SKIP_SEND is enabled',
        },
        skipped: { type: 'boolean' },
        type: {
          type: 'string',
          enum: ['onboarding', 'payment_confirmation', 'text'],
        },
        to: {
          type: 'string',
          description: 'Recipient phone (digits only)',
        },
        provider: {
          type: 'string',
          example: 'meta',
          description: 'WHATSAPP_PROVIDER (meta or twilio)',
        },
      },
    },
  })
  @ApiBadRequestResponse({
    description:
      'Missing phone/member_id, member not found, no phone on member, or Meta/Twilio API error',
  })
  @ApiForbiddenResponse({ description: 'No access to manage this gym' })
  @ApiNotFoundResponse({ description: 'Gym not found' })
  send(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Body() body: TestWhatsAppMessageDto,
  ) {
    return this.whatsappTest.sendTest(user.sub, query.gymId, body);
  }
}
