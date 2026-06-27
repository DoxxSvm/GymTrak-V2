import { Body, Controller, Get, Param, Post, Put, Query } from '@nestjs/common';
import { EnquiryStatus } from '@prisma/client';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { EnquiriesService } from './enquiries.service';
import { CreateAdvancedInquiryDto } from './dto/create-advanced-inquiry.dto';
import { InquiryListQueryDto } from './dto/inquiry-list-query.dto';
import { UpdateInquiryStatusDto } from './dto/update-inquiry-status.dto';

@Controller('inquiries')
export class InquiriesCompatController {
  constructor(private readonly enquiries: EnquiriesService) {}

  @Post()
  create(@CurrentUser() user: JwtUser, @Body() body: CreateAdvancedInquiryDto) {
    const fullName = `${body.first_name} ${body.last_name}`.trim();
    const noteBits = [body.notes?.trim()].filter(Boolean);
    return this.enquiries.create(user.sub, {
      gymId: body.gymId,
      name: fullName,
      firstName: body.first_name,
      lastName: body.last_name,
      phone: body.phone,
      photoUrl: body.photo_url,
      source: body.source,
      medium: body.medium ?? body.source,
      interestedIn: body.interested_in ?? body.interest,
      gender: body.gender,
      address: body.address,
      enquiryDate: body.date,
      notes: noteBits.join(' | '),
    });
  }

  @Get()
  async list(
    @CurrentUser() user: JwtUser,
    @Query() query: InquiryListQueryDto,
  ) {
    const mappedStatus =
      query.status === 'pending'
        ? EnquiryStatus.OPEN
        : query.status === 'converted'
          ? EnquiryStatus.CONVERTED
          : query.status === 'lost'
            ? EnquiryStatus.LOST
            : undefined;
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;
    const res = await this.enquiries.list(
      user.sub,
      query.gymId,
      mappedStatus,
      query.q,
      limit,
      offset,
    );
    return res.items.map((i) => ({
      id: i.id,
      name: i.name,
      phone: i.phone,
      status:
        i.status === EnquiryStatus.OPEN
          ? 'pending'
          : i.status === EnquiryStatus.CONVERTED
            ? 'converted'
            : i.status.toLowerCase(),
      created_at: i.createdAt,
    }));
  }

  @Get('stats')
  async stats(@CurrentUser() user: JwtUser, @Query() query: GymIdQueryDto) {
    const s = await this.enquiries.stats(user.sub, query.gymId);
    return {
      total: s.total,
      converted: s.converted,
      pending: s.pending,
    };
  }

  @Put(':id')
  updateStatus(
    @CurrentUser() user: JwtUser,
    @Param('id') id: string,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateInquiryStatusDto,
  ) {
    return this.enquiries.update(user.sub, query.gymId, id, {
      status:
        body.status === 'converted'
          ? EnquiryStatus.CONVERTED
          : EnquiryStatus.LOST,
    });
  }
}
