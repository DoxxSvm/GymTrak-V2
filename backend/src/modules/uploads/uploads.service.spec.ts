jest.mock('fs/promises', () => ({
  mkdir: jest.fn().mockResolvedValue(undefined),
  writeFile: jest.fn().mockResolvedValue(undefined),
}));

import { ConfigService } from '@nestjs/config';
import { mkdir, writeFile } from 'fs/promises';
import { UploadsService } from './uploads.service';

describe('UploadsService', () => {
  let service: UploadsService;

  beforeEach(() => {
    service = new UploadsService({
      get: jest.fn().mockReturnValue('https://gymtrak.app'),
    } as unknown as ConfigService);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('stores an uploaded image and returns a public URL', async () => {
    const result = await service.saveImage({
      originalname: 'John Photo.PNG',
      mimetype: 'image/png',
      size: 12,
      buffer: Buffer.from('image-bytes'),
    });

    expect(mkdir).toHaveBeenCalled();
    expect(writeFile).toHaveBeenCalled();
    expect(result.name).toMatch(/^john-photo-[a-f0-9]{12}\.png$/);
    expect(result.relativePath).toMatch(/^\/uploads\/images\/john-photo-/);
    expect(result.url).toMatch(/^https:\/\/gymtrak\.app\/uploads\/images\//);
    expect(result.contentType).toBe('image/png');
  });

  it('rejects non-image uploads', async () => {
    await expect(
      service.saveImage({
        originalname: 'notes.txt',
        mimetype: 'text/plain',
        size: 4,
        buffer: Buffer.from('test'),
      }),
    ).rejects.toThrow('Only image files are allowed');
  });
});
