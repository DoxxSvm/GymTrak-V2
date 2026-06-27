import { Body, Controller, Get, Patch, Query } from '@nestjs/common';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { UpdateMessageTemplateDto } from './dto/update-message-template.dto';
import { MessageTemplatesService } from './message-templates.service';

/** Low-level template CRUD. Mobile app should use `GET/PUT /whatsapp/automation` only. */
@Controller('message-templates')
export class MessageTemplatesController {
  constructor(private readonly templates: MessageTemplatesService) {}

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
}
