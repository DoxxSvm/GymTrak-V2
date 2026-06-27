import { Body, Controller, Get, Put, Query } from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiBody,
  ApiOperation,
  ApiQuery,
  ApiTags,
} from '@nestjs/swagger';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { UpdateWhatsAppAutomationDto } from './dto/whatsapp-automation.dto';
import { MessageTemplatesService } from './message-templates.service';

/**
 * Single API surface for the mobile **Whatsapp Automation → Message Templates** screen.
 */
@ApiTags('WhatsApp')
@ApiBearerAuth()
@Controller('whatsapp/automation')
export class WhatsAppAutomationController {
  constructor(private readonly templates: MessageTemplatesService) {}

  @Get()
  @ApiOperation({
    summary: 'WhatsApp automation settings (Message Templates screen)',
    description:
      'Single API for the owner app WhatsApp Automation screen. Returns five toggles; when `enabled` is true the backend automatically sends WhatsApp on: member joined (onboarding welcome + custom `message`), 7 days before plan expiry, 3 days before expiry, day after expiry, and payment completed. Requires Redis/BullMQ (`DISABLE_BULLMQ` must be false) for automatic delivery.',
  })
  @ApiQuery({ name: 'gymId', required: true, description: 'Gym scope' })
  getSettings(@CurrentUser() user: JwtUser, @Query() query: GymIdQueryDto) {
    return this.templates.getAutomationScreen(user.sub, query.gymId);
  }

  @Put()
  @ApiOperation({
    summary: 'Save WhatsApp automation settings',
    description:
      'Update toggles (and optional onboarding welcome `message`) for the gym. Pass `templates` array with one entry per row on the screen (all five recommended).',
  })
  @ApiQuery({ name: 'gymId', required: true, description: 'Gym scope' })
  @ApiBody({ type: UpdateWhatsAppAutomationDto })
  saveSettings(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateWhatsAppAutomationDto,
  ) {
    return this.templates.saveAutomationScreen(
      user.sub,
      query.gymId,
      body.templates,
    );
  }
}
