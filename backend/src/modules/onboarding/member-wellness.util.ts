/** Activity tier from consumer onboarding (matches typical TDEE multipliers). */
export type ActivityLevelTier = 'LOW' | 'MODERATE' | 'HIGH';

const ACTIVITY_FACTORS: Record<ActivityLevelTier, number> = {
  LOW: 1.2,
  MODERATE: 1.55,
  HIGH: 1.725,
};

export type GenderForBmr = 'MALE' | 'FEMALE' | 'OTHER';

export interface MemberWellnessInput {
  heightCm: number;
  weightKg: number;
  ageYears?: number;
  gender?: GenderForBmr;
  activityLevel?: ActivityLevelTier;
}

export interface MemberWellnessResult {
  bmi: number;
  bmiCategory: 'underweight' | 'normal' | 'overweight' | 'obese';
  /** Mifflin–St Jeor × activity; null if age or gender missing */
  maintenanceCalories: number | null;
}

function bmiCategory(bmi: number): MemberWellnessResult['bmiCategory'] {
  if (bmi < 18.5) return 'underweight';
  if (bmi < 25) return 'normal';
  if (bmi < 30) return 'overweight';
  return 'obese';
}

/** BMR (kcal/day) via Mifflin–St Jeor; `OTHER` uses average of male/female constants. */
function mifflinStJeorBmr(
  weightKg: number,
  heightCm: number,
  ageYears: number,
  gender: GenderForBmr,
): number {
  const base = 10 * weightKg + 6.25 * heightCm - 5 * ageYears;
  if (gender === 'MALE') return base + 5;
  if (gender === 'FEMALE') return base - 161;
  return base + (5 - 161) / 2;
}

export function computeMemberWellness(
  input: MemberWellnessInput,
): MemberWellnessResult {
  const hM = input.heightCm / 100;
  const bmiRaw = hM > 0 ? input.weightKg / (hM * hM) : 0;
  const bmi = Math.round(bmiRaw * 10) / 10;

  let maintenanceCalories: number | null = null;
  if (
    input.ageYears != null &&
    input.gender != null &&
    input.activityLevel != null
  ) {
    const bmr = mifflinStJeorBmr(
      input.weightKg,
      input.heightCm,
      input.ageYears,
      input.gender,
    );
    const factor = ACTIVITY_FACTORS[input.activityLevel] ?? 1.2;
    maintenanceCalories = Math.round(bmr * factor);
  }

  return {
    bmi,
    bmiCategory: bmiCategory(bmiRaw),
    maintenanceCalories,
  };
}
