import { Transform } from 'class-transformer';
import { IsInt, IsString, Matches, Max, Min } from 'class-validator';

function shiftSource(obj: unknown): Record<string, unknown> {
  return obj && typeof obj === 'object' ? (obj as Record<string, unknown>) : {};
}

export class TrainerShiftDto {
  /// Compat: accepts Monday-indexed 0=Mon..6=Sun and stores as DB 0=Sun..6=Sat.
  /// Also accepts legacy `day_of_week` alias.
  @Transform(({ value, obj }) => {
    const o = shiftSource(obj);
    const raw = value ?? o.day_of_week ?? o.dayOfWeek;
    if (raw === undefined || raw === null || raw === '') return undefined;
    const n =
      typeof raw === 'number' && Number.isInteger(raw)
        ? raw
        : parseInt(String(raw).trim(), 10);
    if (!Number.isFinite(n)) return raw;
    // Monday-indexed (0=Mon..6=Sun) → DB (0=Sun..6=Sat)
    return (n + 1) % 7;
  })
  @IsInt()
  @Min(0)
  @Max(6)
  dayOfWeek: number;

  @Transform(({ value, obj }) => {
    const o = shiftSource(obj);
    const raw = value ?? o.start_time ?? o.startTime;
    if (raw === undefined || raw === null) return undefined;
    return String(raw).trim();
  })
  @IsString()
  @Matches(/^\d{1,2}:\d{2}$/)
  startTime: string;

  @Transform(({ value, obj }) => {
    const o = shiftSource(obj);
    const raw = value ?? o.end_time ?? o.endTime;
    if (raw === undefined || raw === null) return undefined;
    return String(raw).trim();
  })
  @IsString()
  @Matches(/^\d{1,2}:\d{2}$/)
  endTime: string;
}
