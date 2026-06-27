import { Injectable } from '@nestjs/common';
import { MessageTemplateKind, Prisma } from '@prisma/client';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaService } from '../prisma/prisma.service';
import { defaultBodyForKind } from './template-copy';

@Injectable()
export class MessageTemplatesService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
  ) {}

  async list(actorUserId: string, gymId: string) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    await this.ensureDefaults(gymId);
    const rows = await this.prisma.gymMessageTemplate.findMany({
      where: { gymId },
      orderBy: { kind: 'asc' },
    });
    return {
      items: rows.map((r) => ({
        kind: r.kind,
        enabled: r.enabled,
        overrideBody: r.overrideBody,
        previewDefault: defaultBodyForKind(r.kind),
      })),
    };
  }

  async update(
    actorUserId: string,
    gymId: string,
    kind: MessageTemplateKind,
    body: { enabled?: boolean; overrideBody?: string | null },
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    await this.ensureDefaults(gymId);
    const data: Prisma.GymMessageTemplateUpdateInput = {};
    if (body.enabled !== undefined) {
      data.enabled = body.enabled;
    }
    if (body.overrideBody !== undefined) {
      data.overrideBody = body.overrideBody?.trim() || null;
    }
    const row = await this.prisma.gymMessageTemplate.update({
      where: { gymId_kind: { gymId, kind } },
      data,
    });
    return {
      kind: row.kind,
      enabled: row.enabled,
      overrideBody: row.overrideBody,
      previewDefault: defaultBodyForKind(row.kind),
    };
  }

  private async ensureDefaults(gymId: string): Promise<void> {
    const skipKinds = new Set<MessageTemplateKind>([
      MessageTemplateKind.EXPIRY_REMINDER,
    ]);
    for (const kind of Object.values(MessageTemplateKind)) {
      if (skipKinds.has(kind)) {
        continue;
      }
      await this.prisma.gymMessageTemplate.upsert({
        where: { gymId_kind: { gymId, kind } },
        create: {
          gymId,
          kind,
          enabled: true,
        },
        update: {},
      });
    }
  }
}
