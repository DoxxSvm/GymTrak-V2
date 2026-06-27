import { IsIn } from 'class-validator';

export class UpdateInquiryStatusDto {
  @IsIn(['converted', 'lost'])
  status: 'converted' | 'lost';
}
