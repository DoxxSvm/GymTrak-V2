import {
  Body,
  Controller,
  Get,
  Param,
  Patch,
  Post,
  Put,
  Query,
} from '@nestjs/common';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { ConvertEnquiryDto } from './dto/convert-enquiry.dto';
import { CreateEnquiryDto } from './dto/create-enquiry.dto';
import { EnquiryListQueryDto } from './dto/enquiry-list-query.dto';
import { UpdateEnquiryDto } from './dto/update-enquiry.dto';
import { EnquiriesService } from './enquiries.service';

@Controller('enquiries')
export class EnquiriesController {
  constructor(private readonly enquiries: EnquiriesService) {}

  @Get()
  list(@CurrentUser() user: JwtUser, @Query() query: EnquiryListQueryDto) {
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;
    return this.enquiries.list(
      user.sub,
      query.gymId,
      query.status,
      query.q,
      limit,
      offset,
    );
  }

  @Get('stats')
  stats(@CurrentUser() user: JwtUser, @Query() query: GymIdQueryDto) {
    return this.enquiries.stats(user.sub, query.gymId);
  }

  @Post()
  create(@CurrentUser() user: JwtUser, @Body() body: CreateEnquiryDto) {
    return this.enquiries.create(user.sub, body);
  }

  @Get(':enquiryId')
  getOne(
    @CurrentUser() user: JwtUser,
    @Param('enquiryId') enquiryId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.enquiries.getOne(user.sub, query.gymId, enquiryId);
  }

  @Patch(':enquiryId')
  update(
    @CurrentUser() user: JwtUser,
    @Param('enquiryId') enquiryId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateEnquiryDto,
  ) {
    return this.enquiries.update(user.sub, query.gymId, enquiryId, body);
  }

  @Put(':enquiryId')
  updateCompat(
    @CurrentUser() user: JwtUser,
    @Param('enquiryId') enquiryId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateEnquiryDto,
  ) {
    return this.enquiries.update(user.sub, query.gymId, enquiryId, body);
  }

  @Post(':enquiryId/convert')
  convert(
    @CurrentUser() user: JwtUser,
    @Param('enquiryId') enquiryId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: ConvertEnquiryDto,
  ) {
    return this.enquiries.convert(user.sub, query.gymId, enquiryId, body);
  }
}
