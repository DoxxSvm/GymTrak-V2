import { Test, TestingModule } from '@nestjs/testing';
import { ConfigService } from '@nestjs/config';
import { AppController } from './app.controller';
import { HealthService } from './modules/health/health.service';

describe('AppController', () => {
  let appController: AppController;
  let healthService: { getOverallHealth: jest.Mock };

  beforeEach(async () => {
    healthService = {
      getOverallHealth: jest.fn(),
    };

    const app: TestingModule = await Test.createTestingModule({
      controllers: [AppController],
      providers: [
        { provide: HealthService, useValue: healthService },
        {
          provide: ConfigService,
          useValue: {
            get: jest.fn((key: string) => {
              if (key === 'APP_NAME') return 'GymTrak';
              if (key === 'APP_VERSION') return '1.0.0';
              return undefined;
            }),
          },
        },
      ],
    }).compile();

    appController = app.get<AppController>(AppController);
  });

  describe('getHealth', () => {
    it('should return overall health payload', async () => {
      const payload = {
        status: 'ok',
        timestamp: new Date().toISOString(),
      };
      healthService.getOverallHealth.mockResolvedValue(payload);

      await expect(appController.getHealth()).resolves.toEqual(payload);
    });
  });

  describe('getAppConfig', () => {
    it('should return mobile app config', () => {
      expect(appController.getAppConfig()).toMatchObject({
        app_name: 'GymTrak',
        app_version: '1.0.0',
      });
    });
  });
});
