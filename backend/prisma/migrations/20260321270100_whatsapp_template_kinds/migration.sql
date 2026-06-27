-- WhatsApp automation: new template kinds (must commit before use in a later migration)
ALTER TYPE "MessageTemplateKind" ADD VALUE 'EXPIRY_REMINDER_7D';
ALTER TYPE "MessageTemplateKind" ADD VALUE 'EXPIRY_REMINDER_3D';
ALTER TYPE "MessageTemplateKind" ADD VALUE 'POST_EXPIRY';
