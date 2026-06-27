/**
 * Mobile-facing unified profile contract (owner + trainer).
 * Maps from User, Gym, GymUser, TrainerProfile — no Prisma changes required.
 */

export type UnifiedProfileRole = 'gym_owner' | 'trainer';

export type UnifiedSalaryDuration = 'month' | 'week' | 'year';

export interface UnifiedPersonalInfo {
  firstName: string;
  lastName: string;
  fullName: string;
  phone: string;
  email: string;
  profileImage: string | null;
  dateOfBirth: string | null;
  gender: 'male' | 'female' | 'other' | null;
  address: string | null;
}

export interface UnifiedGymDetails {
  gymName: string;
  gymAddress: string | null;
  gstNumber: string | null;
  gymLogo: string | null;
}

export interface UnifiedTrainerShift {
  id: string;
  name: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
}

export interface UnifiedTrainerDetails {
  experience: string | null;
  salary: number | null;
  salaryDuration: UnifiedSalaryDuration | null;
  expertise: string[];
  shifts: UnifiedTrainerShift[];
}

export interface UnifiedProfileData {
  id: string;
  role: UnifiedProfileRole;
  personalInfo: UnifiedPersonalInfo;
  gymDetails: UnifiedGymDetails | null;
  trainerDetails: UnifiedTrainerDetails | null;
  createdAt: string;
  updatedAt: string;
}

export interface UnifiedProfileEnvelope {
  success: true;
  data: UnifiedProfileData;
}
