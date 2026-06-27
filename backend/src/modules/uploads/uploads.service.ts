import { BadRequestException, Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { randomBytes } from 'crypto';
import { mkdir, writeFile } from 'fs/promises';
import { extname, join } from 'path';

type UploadedImage = {
  originalname: string;
  mimetype: string;
  size: number;
  buffer: Buffer;
};

@Injectable()
export class UploadsService {
  constructor(private readonly config: ConfigService) {}

  async saveImage(file: UploadedImage) {
    if (!file) {
      throw new BadRequestException('Image file is required');
    }
    if (!file.mimetype?.startsWith('image/')) {
      throw new BadRequestException('Only image files are allowed');
    }
    if (!file.buffer?.length) {
      throw new BadRequestException('Uploaded image is empty');
    }

    const filename = this.buildFilename(file.originalname, file.mimetype);
    const relativePath = `/uploads/images/${filename}`;
    const targetDirectory = join(process.cwd(), 'uploads', 'images');
    await mkdir(targetDirectory, { recursive: true });
    await writeFile(join(targetDirectory, filename), file.buffer);

    return {
      name: filename,
      url: this.toPublicUrl(relativePath),
      relativePath,
      contentType: file.mimetype,
      size: file.size,
    };
  }

  private buildFilename(originalname: string, mimetype: string) {
    const providedExt = extname(originalname || '').toLowerCase();
    const ext = providedExt || this.extFromMime(mimetype);
    const base = (originalname || 'image')
      .replace(/\.[^/.]+$/, '')
      .trim()
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .slice(0, 40);
    const safeBase = base || 'image';
    const suffix = randomBytes(6).toString('hex');
    return `${safeBase}-${suffix}${ext}`;
  }

  private extFromMime(mimetype: string) {
    switch (mimetype) {
      case 'image/jpeg':
        return '.jpg';
      case 'image/png':
        return '.png';
      case 'image/webp':
        return '.webp';
      case 'image/gif':
        return '.gif';
      default:
        return '.bin';
    }
  }

  private toPublicUrl(relativePath: string) {
    const publicBase = this.config.get<string>('APP_PUBLIC_URL')?.trim();
    if (!publicBase) {
      return relativePath;
    }
    return `${publicBase.replace(/\/$/, '')}${relativePath}`;
  }
}
