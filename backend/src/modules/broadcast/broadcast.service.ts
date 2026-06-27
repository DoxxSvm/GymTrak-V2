import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { GymRole, type Prisma } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { GymAccessService } from '../../common/services/gym-access.service';
import { AddBroadcastMembersDto } from './dto/add-broadcast-members.dto';
import { CreateBroadcastChannelDto } from './dto/create-broadcast-channel.dto';
import { CreateBroadcastMessageDto } from './dto/create-broadcast-message.dto';
import { ListBroadcastChannelsQueryDto } from './dto/list-broadcast-channels.query.dto';
import { ListBroadcastMembersQueryDto } from './dto/list-broadcast-members.query.dto';
import { ListBroadcastMemberPickerQueryDto } from './dto/list-broadcast-member-picker.query.dto';
import { ListBroadcastMessagesQueryDto } from './dto/list-broadcast-messages.query.dto';
import { UpdateBroadcastChannelDto } from './dto/update-broadcast-channel.dto';

const defaultPage = 1;
const defaultLimit = 20;

@Injectable()
export class BroadcastService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
  ) {}

  private async canManageGym(userId: string, gymId: string): Promise<boolean> {
    try {
      await this.gymAccess.assertCanManageGym(userId, gymId);
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Single-query access check: resolves channel gymId + user's gymUserId + membership in one shot.
   * Returns gymId for further use; throws if not accessible.
   */
  private async resolveChannelAccess(
    userId: string,
    channelId: string,
  ): Promise<{ gymId: string; canManage: boolean }> {
    const ch = await this.prisma.broadcastChannel.findUnique({
      where: { id: channelId },
      select: { id: true, gymId: true },
    });
    if (!ch) {
      throw new NotFoundException('Channel not found');
    }
    const manage = await this.canManageGym(userId, ch.gymId);
    if (manage) {
      return { gymId: ch.gymId, canManage: true };
    }
    const membership = await this.prisma.broadcastChannelMember.findFirst({
      where: {
        channelId,
        gymUser: { userId, gymId: ch.gymId, isActive: true },
      },
      select: { id: true },
    });
    if (!membership) {
      throw new ForbiddenException('No access to this channel');
    }
    return { gymId: ch.gymId, canManage: false };
  }

  async listChannels(
    userId: string,
    gymId: string,
    query: ListBroadcastChannelsQueryDto,
  ) {
    const page = query.page ?? defaultPage;
    const limit = query.limit ?? defaultLimit;
    const search = query.search?.trim();
    const nameFilter: Prisma.StringFilter | undefined = search
      ? { contains: search, mode: 'insensitive' as const }
      : undefined;

    const [manage, gymUser] = await Promise.all([
      this.canManageGym(userId, gymId),
      this.prisma.gymUser.findFirst({
        where: { gymId, userId, isActive: true },
        select: { id: true },
      }),
    ]);

    if (!manage && !gymUser) {
      throw new ForbiddenException('No access to this gym');
    }

    const where: Prisma.BroadcastChannelWhereInput = {
      gymId,
      ...(nameFilter ? { name: nameFilter } : {}),
      ...(!manage && gymUser
        ? { members: { some: { gymUserId: gymUser.id } } }
        : {}),
    };

    const [total, rows] = await Promise.all([
      this.prisma.broadcastChannel.count({ where }),
      this.prisma.broadcastChannel.findMany({
        where,
        orderBy: { updatedAt: 'desc' },
        skip: (page - 1) * limit,
        take: limit,
        select: {
          id: true,
          name: true,
          description: true,
          imageUrl: true,
          createdAt: true,
          updatedAt: true,
          _count: { select: { members: true, messages: true } },
          messages: {
            orderBy: { createdAt: 'desc' },
            take: 1,
            select: {
              id: true,
              title: true,
              description: true,
              imageUrl: true,
              createdAt: true,
            },
          },
        },
      }),
    ]);

    return {
      page,
      limit,
      total,
      data: rows.map((r) => {
        const last = r.messages[0];
        const lastPreviewRaw = last
          ? [last.title, last.description]
              .filter(Boolean)
              .join(' ')
              .replace(/\s+/g, ' ')
              .trim()
          : '';
        return {
          id: r.id,
          name: r.name,
          description: r.description,
          imageUrl: r.imageUrl,
          createdAt: r.createdAt,
          updatedAt: r.updatedAt,
          memberCount: r._count.members,
          messageCount: r._count.messages,
          lastMessage: last
            ? {
                id: last.id,
                title: last.title,
                description: last.description,
                imageUrl: last.imageUrl,
                createdAt: last.createdAt,
                preview:
                  lastPreviewRaw.length > 200
                    ? `${lastPreviewRaw.slice(0, 200)}…`
                    : lastPreviewRaw,
              }
            : null,
        };
      }),
    };
  }

  async getChannelById(userId: string, channelId: string) {
    await this.resolveChannelAccess(userId, channelId);
    const ch = await this.prisma.broadcastChannel.findUniqueOrThrow({
      where: { id: channelId },
      select: {
        id: true,
        gymId: true,
        name: true,
        description: true,
        imageUrl: true,
        createdAt: true,
        updatedAt: true,
        createdBy: {
          select: { id: true, fullName: true, avatarUrl: true, phone: true },
        },
        _count: { select: { members: true, messages: true } },
      },
    });
    return {
      id: ch.id,
      gymId: ch.gymId,
      name: ch.name,
      description: ch.description,
      imageUrl: ch.imageUrl,
      createdAt: ch.createdAt,
      updatedAt: ch.updatedAt,
      createdBy: ch.createdBy,
      memberCount: ch._count.members,
      messageCount: ch._count.messages,
    };
  }

  async createChannel(userId: string, dto: CreateBroadcastChannelDto) {
    const { gymId } = dto;
    const [, creatorGymUserId] = await Promise.all([
      this.gymAccess.assertCanManageGym(userId, gymId),
      this.prisma.gymUser
        .findFirst({
          where: { gymId, userId, isActive: true },
          select: { id: true },
        })
        .then((r) => r?.id ?? null),
    ]);

    const ch = await this.prisma.broadcastChannel.create({
      data: {
        gymId,
        name: dto.name.trim(),
        description: dto.description?.trim() ?? null,
        imageUrl: dto.imageUrl?.trim() ?? null,
        createdByUserId: userId,
        ...(creatorGymUserId
          ? { members: { create: { gymUserId: creatorGymUserId } } }
          : {}),
      },
      select: {
        id: true,
        gymId: true,
        name: true,
        description: true,
        imageUrl: true,
        createdAt: true,
        updatedAt: true,
        createdBy: {
          select: { id: true, fullName: true, avatarUrl: true, phone: true },
        },
        _count: { select: { members: true, messages: true } },
      },
    });

    return {
      id: ch.id,
      gymId: ch.gymId,
      name: ch.name,
      description: ch.description,
      imageUrl: ch.imageUrl,
      createdAt: ch.createdAt,
      updatedAt: ch.updatedAt,
      createdBy: ch.createdBy,
      memberCount: ch._count.members,
      messageCount: ch._count.messages,
    };
  }

  async listMembersList(
    userId: string,
    query: ListBroadcastMemberPickerQueryDto,
  ) {
    const { gymId } = query;
    const q = query.search?.trim();
    await this.gymAccess.assertCanBrowseGymCatalog(userId, gymId);
    const userFilter: Prisma.UserWhereInput | undefined = q
      ? {
          OR: [
            { fullName: { contains: q, mode: 'insensitive' } },
            { phone: { contains: q } },
            { username: { contains: q, mode: 'insensitive' } },
          ],
        }
      : undefined;
    const members = await this.prisma.gymUser.findMany({
      where: {
        gymId,
        isActive: true,
        role: GymRole.MEMBER,
        ...(userFilter ? { user: userFilter } : {}),
      },
      orderBy: { user: { fullName: 'asc' } },
      take: 200,
      select: {
        id: true,
        user: {
          select: {
            fullName: true,
            phone: true,
            username: true,
            avatarUrl: true,
          },
        },
      },
    });
    return members;
  }

  async updateChannel(
    userId: string,
    channelId: string,
    dto: UpdateBroadcastChannelDto,
  ) {
    const ch = await this.prisma.broadcastChannel.findUnique({
      where: { id: channelId },
      select: { id: true, gymId: true },
    });
    if (!ch) {
      throw new NotFoundException('Channel not found');
    }
    await this.gymAccess.assertCanManageGym(userId, ch.gymId);

    if (
      dto.name == null &&
      dto.description == null &&
      dto.imageUrl === undefined
    ) {
      throw new BadRequestException('Nothing to update');
    }

    const updated = await this.prisma.broadcastChannel.update({
      where: { id: channelId },
      data: {
        ...(dto.name != null && { name: dto.name.trim() }),
        ...(dto.description !== undefined && {
          description:
            dto.description === null || dto.description === ''
              ? null
              : dto.description.trim(),
        }),
        ...(dto.imageUrl !== undefined && {
          imageUrl:
            dto.imageUrl === null || dto.imageUrl === ''
              ? null
              : dto.imageUrl.trim(),
        }),
      },
      select: {
        id: true,
        gymId: true,
        name: true,
        description: true,
        imageUrl: true,
        createdAt: true,
        updatedAt: true,
        createdBy: {
          select: { id: true, fullName: true, avatarUrl: true, phone: true },
        },
        _count: { select: { members: true, messages: true } },
      },
    });

    return {
      id: updated.id,
      gymId: updated.gymId,
      name: updated.name,
      description: updated.description,
      imageUrl: updated.imageUrl,
      createdAt: updated.createdAt,
      updatedAt: updated.updatedAt,
      createdBy: updated.createdBy,
      memberCount: updated._count.members,
      messageCount: updated._count.messages,
    };
  }

  async deleteChannel(userId: string, channelId: string) {
    const ch = await this.prisma.broadcastChannel.findUnique({
      where: { id: channelId },
      select: { id: true, gymId: true },
    });
    if (!ch) {
      throw new NotFoundException('Channel not found');
    }
    await this.gymAccess.assertCanManageGym(userId, ch.gymId);
    await this.prisma.broadcastChannel.delete({ where: { id: channelId } });
    return { success: true as const };
  }

  async addMembers(
    userId: string,
    channelId: string,
    body: AddBroadcastMembersDto,
  ) {
    const ch = await this.prisma.broadcastChannel.findUnique({
      where: { id: channelId },
      select: { gymId: true },
    });
    if (!ch) {
      throw new NotFoundException('Channel not found');
    }
    await this.gymAccess.assertCanManageGym(userId, ch.gymId);

    const uniqueIds = [...new Set(body.gymUserIds)];
    const gymUserRows = await this.prisma.gymUser.findMany({
      where: {
        id: { in: uniqueIds },
        gymId: ch.gymId,
        isActive: true,
      },
      select: { id: true },
    });
    if (gymUserRows.length !== uniqueIds.length) {
      const found = new Set(gymUserRows.map((g) => g.id));
      const missing = uniqueIds.filter((id) => !found.has(id));
      throw new BadRequestException(
        `Unknown or inactive gym user id(s) for this gym: ${missing.slice(0, 5).join(', ')}${missing.length > 5 ? '…' : ''}`,
      );
    }
    const createRes = await this.prisma.broadcastChannelMember.createMany({
      data: uniqueIds.map((gymUserId) => ({ channelId, gymUserId })),
      skipDuplicates: true,
    });
    return { added: createRes.count, requested: uniqueIds.length };
  }

  async removeMember(userId: string, channelId: string, gymUserId: string) {
    const ch = await this.prisma.broadcastChannel.findUnique({
      where: { id: channelId },
      select: { gymId: true },
    });
    if (!ch) {
      throw new NotFoundException('Channel not found');
    }
    await this.gymAccess.assertCanManageGym(userId, ch.gymId);

    const deleted = await this.prisma.broadcastChannelMember.deleteMany({
      where: { channelId, gymUserId },
    });
    if (deleted.count === 0) {
      throw new NotFoundException('Member not in this channel');
    }
    return { success: true as const };
  }

  async listMembers(
    userId: string,
    channelId: string,
    query: ListBroadcastMembersQueryDto,
  ) {
    const { canManage } = await this.resolveChannelAccess(userId, channelId);
    void canManage;

    const page = query.page ?? defaultPage;
    const limit = query.limit ?? defaultLimit;
    const search = query.search?.trim();
    const userWhere: Prisma.UserWhereInput | undefined = search
      ? {
          OR: [
            { fullName: { contains: search, mode: 'insensitive' } },
            { phone: { contains: search } },
          ],
        }
      : undefined;
    const where: Prisma.BroadcastChannelMemberWhereInput = {
      channelId,
      gymUser: {
        isActive: true,
        ...(userWhere ? { user: userWhere } : {}),
      },
    };

    const [total, members] = await Promise.all([
      this.prisma.broadcastChannelMember.count({ where }),
      this.prisma.broadcastChannelMember.findMany({
        where,
        orderBy: { joinedAt: 'asc' },
        skip: (page - 1) * limit,
        take: limit,
        select: {
          id: true,
          joinedAt: true,
          gymUserId: true,
          gymUser: {
            select: {
              user: {
                select: {
                  id: true,
                  fullName: true,
                  phone: true,
                  avatarUrl: true,
                },
              },
            },
          },
        },
      }),
    ]);
    return {
      page,
      limit,
      total,
      data: members.map((m) => ({
        membershipId: m.id,
        joinedAt: m.joinedAt,
        gymUserId: m.gymUserId,
        user: m.gymUser.user,
      })),
    };
  }

  async createMessage(
    userId: string,
    channelId: string,
    dto: CreateBroadcastMessageDto,
  ) {
    const ch = await this.prisma.broadcastChannel.findUnique({
      where: { id: channelId },
      select: { gymId: true },
    });
    if (!ch) {
      throw new NotFoundException('Channel not found');
    }
    await this.gymAccess.assertCanManageGym(userId, ch.gymId);

    const [row] = await Promise.all([
      this.prisma.broadcastMessage.create({
        data: {
          channelId,
          title: dto.title.trim(),
          description: dto.description?.trim() ?? null,
          imageUrl: dto.imageUrl?.trim() ?? null,
          createdByUserId: userId,
        },
      }),
      this.prisma.broadcastChannel.update({
        where: { id: channelId },
        data: { updatedAt: new Date() },
      }),
    ]);
    return row;
  }

  async listMessages(
    userId: string,
    channelId: string,
    query: ListBroadcastMessagesQueryDto,
  ) {
    const { canManage } = await this.resolveChannelAccess(userId, channelId);
    void canManage;

    const page = query.page ?? defaultPage;
    const limit = query.limit ?? defaultLimit;

    const where: Prisma.BroadcastMessageWhereInput = { channelId };
    const [total, data] = await Promise.all([
      this.prisma.broadcastMessage.count({ where }),
      this.prisma.broadcastMessage.findMany({
        where,
        orderBy: { createdAt: 'desc' },
        skip: (page - 1) * limit,
        take: limit,
        select: {
          id: true,
          title: true,
          description: true,
          imageUrl: true,
          createdAt: true,
          createdBy: {
            select: { id: true, fullName: true, avatarUrl: true, phone: true },
          },
        },
      }),
    ]);
    return { page, limit, total, data };
  }
}
