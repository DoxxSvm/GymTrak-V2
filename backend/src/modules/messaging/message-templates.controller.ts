import {
  BadRequestException,
  Body,
  Controller,
  Get,
  Patch,
  Post,
  Put,
  Query,
} from '@nestjs/common';
import { GymRole, MessageTemplateKind } from '@prisma/client';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { UpdateMessageTemplateDto } from './dto/update-message-template.dto';
import { MessageTemplatesService } from './message-templates.service';
import { WhatsAppAutomationService } from './whatsapp-automation.service';
import { PrismaService } from '../prisma/prisma.service';

@Controller('message-templates')
export class MessageTemplatesController {
  constructor(
    private readonly templates: MessageTemplatesService,
    private readonly whatsapp: WhatsAppAutomationService,
    private readonly prisma: PrismaService,
  ) {}

  @Get()
  list(@CurrentUser() user: JwtUser, @Query() query: GymIdQueryDto) {
    return this.templates.list(user.sub, query.gymId);
  }

  @Patch()
  update(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateMessageTemplateDto,
  ) {
    const { kind, enabled, overrideBody } = body;
    return this.templates.update(user.sub, query.gymId, kind, {
      enabled,
      overrideBody,
    });
  }

  @Get('/automation/templates')
  async automationTemplates(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
  ) {
    const res = await this.templates.list(user.sub, query.gymId);
    const lookup = new Map(res.items.map((i) => [i.kind, i.enabled]));
    return {
      onboarding: lookup.get(MessageTemplateKind.WELCOME) ?? true,
      expiry_reminder:
        lookup.get(MessageTemplateKind.EXPIRY_REMINDER_7D) ?? true,
      payment_received:
        lookup.get(MessageTemplateKind.PAYMENT_CONFIRMATION) ?? true,
    };
  }

  @Put('/automation/templates')
  async updateAutomationTemplates(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Body()
    body: {
      onboarding?: boolean;
      expiry_reminder?: boolean;
      payment_received?: boolean;
    },
  ) {
    const updates = [];
    if (body.onboarding !== undefined) {
      updates.push(
        this.templates.update(
          user.sub,
          query.gymId,
          MessageTemplateKind.WELCOME,
          {
            enabled: body.onboarding,
          },
        ),
      );
    }
    if (body.expiry_reminder !== undefined) {
      updates.push(
        this.templates.update(
          user.sub,
          query.gymId,
          MessageTemplateKind.EXPIRY_REMINDER_7D,
          { enabled: body.expiry_reminder },
        ),
      );
    }
    if (body.payment_received !== undefined) {
      updates.push(
        this.templates.update(
          user.sub,
          query.gymId,
          MessageTemplateKind.PAYMENT_CONFIRMATION,
          { enabled: body.payment_received },
        ),
      );
    }
    await Promise.all(updates);
    return this.automationTemplates(user, query);
  }

  @Post('/automation/send')
  async sendAutomation(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Body()
    body: {
      type: 'expiry_reminder' | 'onboarding' | 'payment_received';
      member_id: string;
    },
  ) {
    const member = await this.prisma.gymUser.findFirst({
      where: { id: body.member_id, gymId: query.gymId, role: GymRole.MEMBER },
      select: { userId: true },
    });
    if (!member) {
      throw new BadRequestException('Member not found');
    }
    await this.templates.list(user.sub, query.gymId);
    if (body.type === 'expiry_reminder') {
      await this.whatsapp.enqueueExpiryReminder(query.gymId, member.userId);
    } else if (body.type === 'onboarding') {
      await this.whatsapp.enqueueWelcome(query.gymId, member.userId);
    } else {
      await this.whatsapp.enqueuePaymentConfirmation(
        query.gymId,
        member.userId,
        'manual',
      );
    }
    return { queued: true };
  }
}
