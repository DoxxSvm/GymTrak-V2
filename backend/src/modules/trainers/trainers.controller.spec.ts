import { TrainersController } from './trainers.controller';

describe('TrainersController', () => {
  let controller: TrainersController;
  let trainers: { create: jest.Mock };

  beforeEach(() => {
    trainers = {
      create: jest.fn(),
    };
    controller = new TrainersController(trainers as never);
  });

  it('maps compat trainer payload into the main create flow', async () => {
    trainers.create.mockResolvedValue({ ok: true });

    await controller.createCompat({ sub: 'owner-1' } as never, {
      gymId: 'gym-1',
      phone: '9999999999',
      full_name: 'John',
      dob: '2026-05-04',
      gender: 'male',
      experience: '5+ years',
      address: 'At-Rajkot',
      profile_image: '/uploads/images/john.png',
      salary: 50,
      salary_type: 'monthly',
      expertise: ['Strength', 'Yoga'],
      shift: {
        days: ['Monday', 'Wednesday'],
        start_time: '08:00',
        end_time: '09:00',
      },
      credentials: {
        trainer_id: '#9509000',
        password: '1212@522',
      },
      permissions: {
        add_clients: true,
        show_dashboard: true,
        show_payments: false,
        show_payment_in_details: true,
        add_trainer: true,
      },
    });

    expect(trainers.create).toHaveBeenCalledWith('owner-1', {
      gymId: 'gym-1',
      phone: '9999999999',
      fullName: 'John',
      avatarUrl: '/uploads/images/john.png',
      dateOfBirth: '2026-05-04',
      gender: 'male',
      experience: '5+ years',
      address: 'At-Rajkot',
      expertise: ['Strength', 'Yoga'],
      salaryCents: 5000,
      salaryPeriod: 'MONTHLY',
      notes: undefined,
      shifts: [
        { dayOfWeek: 1, startTime: '08:00', endTime: '09:00' },
        { dayOfWeek: 3, startTime: '08:00', endTime: '09:00' },
      ],
      permissions: {
        members: true,
        dashboard: true,
        payments: true,
        admin: true,
      },
      generateLoginCredentials: false,
      username: '#9509000',
      password: '1212@522',
    });
  });
});
