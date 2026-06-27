import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  Param,
  Patch,
  Post,
  Query,
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiBody,
  ApiOperation,
  ApiParam,
  ApiQuery,
  ApiTags,
} from '@nestjs/swagger';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { BroadcastService } from './broadcast.service';
import { AddBroadcastMembersDto } from './dto/add-broadcast-members.dto';
import { CreateBroadcastChannelDto } from './dto/create-broadcast-channel.dto';
import { CreateBroadcastMessageDto } from './dto/create-broadcast-message.dto';
import { ListBroadcastChannelsQueryDto } from './dto/list-broadcast-channels.query.dto';
import { ListBroadcastMemberPickerQueryDto } from './dto/list-broadcast-member-picker.query.dto';
import { ListBroadcastMembersQueryDto } from './dto/list-broadcast-members.query.dto';
import { ListBroadcastMessagesQueryDto } from './dto/list-broadcast-messages.query.dto';
import { UpdateBroadcastChannelDto } from './dto/update-broadcast-channel.dto';

@ApiTags('Broadcast')
@ApiBearerAuth()
@Controller('broadcast')
export class BroadcastController {
  constructor(private readonly broadcast: BroadcastService) {}

  @Get('channels')
  @ApiOperation({
    summary: 'List broadcast channels (My channels)',
    description:
      '**Gym must be in scope** (`gymId`). **Owner / staff / trainer:** all channels in the gym. ' +
      '**Members and others** with active `GymUser` at the gym: only channels where that membership is subscribed. ' +
      'Each item includes a **lastMessage** preview and counts for the list UI.',
  })
  @ApiQuery({
    name: 'gymId',
    required: true,
    description: 'Gym to list channels for',
  })
  @ApiQuery({ name: 'page', required: false, example: 1 })
  @ApiQuery({ name: 'limit', required: false, example: 20 })
  @ApiQuery({
    name: 'search',
    required: false,
    description: 'Name contains (case-insensitive)',
  })
  listChannels(
    @CurrentUser() user: JwtUser,
    @Query() query: ListBroadcastChannelsQueryDto,
  ) {
    return this.broadcast.listChannels(user.sub, query.gymId, query);
  }

  @Get('channels/members-list')
  @ApiOperation({
    summary: 'List members for add-to-broadcast picker',
    description:
      'Active **`GymUser`** (role MEMBER) for `gymId`: `id` for `POST .../members` **`gymUserIds`**, and **`user`**: `fullName`, `phone`, `username`, `avatarUrl`. ' +
      'Optional **`search`**: case-insensitive substring on `fullName` and `username`, substring on `phone`. ' +
      'Caller must **browse the gym** (`assertCanBrowseGymCatalog`).',
  })
  @ApiQuery({
    name: 'gymId',
    required: true,
    description: 'Gym to list members for',
  })
  @ApiQuery({
    name: 'search',
    required: false,
    description: 'Filter by name, phone, or username (partial match)',
  })
  listMembersList(
    @CurrentUser() user: JwtUser,
    @Query() query: ListBroadcastMemberPickerQueryDto,
  ) {
    return this.broadcast.listMembersList(user.sub, query);
  }

  @Post('channels')
  @HttpCode(201)
  @ApiOperation({
    summary: 'Create broadcast channel',
    description:
      '**Manage gym** (owner, active staff, trainer) only. ' +
      'The creator is added as a **member** if they have an active `GymUser` at this gym. ' +
      'Use **PATCH** to set image/description after upload.',
  })
  @ApiBody({ type: CreateBroadcastChannelDto })
  createChannel(
    @CurrentUser() user: JwtUser,
    @Body() body: CreateBroadcastChannelDto,
  ) {
    return this.broadcast.createChannel(user.sub, body);
  }

  @Get('channels/:channelId')
  @ApiOperation({
    summary: 'Get broadcast channel (settings / detail)',
    description:
      '**Members** of the channel or **staff** managing the gym. Returns metadata and creator (no full member list — use `GET .../members`).',
  })
  @ApiParam({ name: 'channelId' })
  getChannel(
    @CurrentUser() user: JwtUser,
    @Param('channelId') channelId: string,
  ) {
    return this.broadcast.getChannelById(user.sub, channelId);
  }

  @Patch('channels/:channelId')
  @ApiOperation({
    summary: 'Update channel profile (name, description, image URL)',
  })
  @ApiParam({ name: 'channelId' })
  @ApiBody({ type: UpdateBroadcastChannelDto })
  updateChannel(
    @CurrentUser() user: JwtUser,
    @Param('channelId') channelId: string,
    @Body() body: UpdateBroadcastChannelDto,
  ) {
    return this.broadcast.updateChannel(user.sub, channelId, body);
  }

  @Delete('channels/:channelId')
  @ApiOperation({ summary: 'Delete broadcast channel' })
  @ApiParam({ name: 'channelId' })
  deleteChannel(
    @CurrentUser() user: JwtUser,
    @Param('channelId') channelId: string,
  ) {
    return this.broadcast.deleteChannel(user.sub, channelId);
  }

  @Post('channels/:channelId/members')
  @HttpCode(201)
  @ApiOperation({
    summary: 'Add members to broadcast channel',
    description:
      'Pass **`gymUserIds`**: `GymUser.id` for users at the same gym (e.g. from the member list / search in the app).',
  })
  @ApiParam({ name: 'channelId' })
  @ApiBody({ type: AddBroadcastMembersDto })
  addMembers(
    @CurrentUser() user: JwtUser,
    @Param('channelId') channelId: string,
    @Body() body: AddBroadcastMembersDto,
  ) {
    return this.broadcast.addMembers(user.sub, channelId, body);
  }

  @Get('channels/:channelId/members')
  @ApiOperation({
    summary: 'List members in a broadcast channel',
    description:
      '**Search** matches full name or phone. Paginated for “View all (N more)”.',
  })
  @ApiParam({ name: 'channelId' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @ApiQuery({ name: 'search', required: false })
  listMembers(
    @CurrentUser() user: JwtUser,
    @Param('channelId') channelId: string,
    @Query() query: ListBroadcastMembersQueryDto,
  ) {
    return this.broadcast.listMembers(user.sub, channelId, query);
  }

  @Delete('channels/:channelId/members/:gymUserId')
  @ApiOperation({ summary: 'Remove a member from the channel' })
  @ApiParam({ name: 'channelId' })
  @ApiParam({ name: 'gymUserId' })
  removeMember(
    @CurrentUser() user: JwtUser,
    @Param('channelId') channelId: string,
    @Param('gymUserId') gymUserId: string,
  ) {
    return this.broadcast.removeMember(user.sub, channelId, gymUserId);
  }

  @Post('channels/:channelId/messages')
  @HttpCode(201)
  @ApiOperation({
    summary: 'Create a broadcast message',
    description:
      '**Staff-only** (same as channel create). Pushes a card with **title**, optional **description** and **image** to subscribers for this channel.',
  })
  @ApiParam({ name: 'channelId' })
  @ApiBody({ type: CreateBroadcastMessageDto })
  createMessage(
    @CurrentUser() user: JwtUser,
    @Param('channelId') channelId: string,
    @Body() body: CreateBroadcastMessageDto,
  ) {
    return this.broadcast.createMessage(user.sub, channelId, body);
  }

  @Get('channels/:channelId/messages')
  @ApiOperation({ summary: 'List broadcast channel messages' })
  @ApiParam({ name: 'channelId' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  listMessages(
    @CurrentUser() user: JwtUser,
    @Param('channelId') channelId: string,
    @Query() query: ListBroadcastMessagesQueryDto,
  ) {
    return this.broadcast.listMessages(user.sub, channelId, query);
  }
}
