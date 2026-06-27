import { Injectable } from '@nestjs/common';
import { MessageTemplateKind, Prisma } from '@prisma/client';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaService } from '../prisma/prisma.service';
import { defaultBodyForKind } from './template-copy';
import {
  automationMetaById,
  WHATSAPP_AUTOMATION_TEMPLATES,
} from './whatsapp-automation.config';
import type { WhatsAppAutomationTemplateItemDto } from './dto/whatsapp-automation.dto';

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

  /** Mobile **Whatsapp Automation → Message Templates** screen. */
  async getAutomationScreen(actorUserId: string, gymId: string) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    await this.ensureDefaults(gymId);
    const rows = await this.prisma.gymMessageTemplate.findMany({
      where: { gymId },
    });
    const byKind = new Map(rows.map((r) => [r.kind, r]));

    return {
      screenTitle: 'Message Templates',
      screenDescription:
        'Configure automated WhatsApp messages for your members. These messages will be sent automatically based on the triggers below.',
      templates: WHATSAPP_AUTOMATION_TEMPLATES.map((meta) => {
        const row = byKind.get(meta.kind);
        const defaultMessage = defaultBodyForKind(meta.kind);
        const overrideBody = row?.overrideBody ?? null;
        return {
          id: meta.id,
          title: meta.title,
          description: meta.description,
          autoTrigger: meta.autoTrigger,
          enabled: row?.enabled ?? true,
          ...(meta.supportsCustomMessage
            ? {
                message: overrideBody ?? defaultMessage,
                defaultMessage,
              }
            : {}),
        };
      }),
    };
  }

  async saveAutomationScreen(
    actorUserId: string,
    gymId: string,
    templates: WhatsAppAutomationTemplateItemDto[],
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    await this.ensureDefaults(gymId);

    const seen = new Set<string>();
    for (const item of templates) {
      if (seen.has(item.id)) {
        continue;
      }
      seen.add(item.id);
      const meta = automationMetaById(item.id);
      if (!meta) {
        continue;
      }
      const data: Prisma.GymMessageTemplateUpdateInput = {
        enabled: item.enabled,
      };
      if (meta.supportsCustomMessage && item.message !== undefined) {
        data.overrideBody = item.message?.trim() || null;
      }
      await this.prisma.gymMessageTemplate.update({
        where: { gymId_kind: { gymId, kind: meta.kind } },
        data,
      });
    }

    return this.getAutomationScreen(actorUserId, gymId);
  }

  /** Used by WhatsApp processor before sending (missing row = enabled). */
  async isTemplateEnabled(
    gymId: string,
    kind: MessageTemplateKind,
  ): Promise<boolean> {
    await this.ensureDefaults(gymId);
    const row = await this.prisma.gymMessageTemplate.findUnique({
      where: { gymId_kind: { gymId, kind } },
      select: { enabled: true },
    });
    return row?.enabled !== false;
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
