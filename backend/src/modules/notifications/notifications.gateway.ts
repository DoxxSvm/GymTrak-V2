import { Logger } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import {
  ConnectedSocket,
  OnGatewayConnection,
  WebSocketGateway,
  WebSocketServer,
} from '@nestjs/websockets';
import type { Server, Socket } from 'socket.io';
import type { JwtPayload } from '../auth/types/jwt-user.type';

@WebSocketGateway({
  namespace: '/notifications',
  cors: { origin: true, credentials: true },
})
export class NotificationsGateway implements OnGatewayConnection {
  private readonly logger = new Logger(NotificationsGateway.name);

  @WebSocketServer()
  server!: Server;

  constructor(private readonly jwt: JwtService) {}

  async handleConnection(@ConnectedSocket() client: Socket) {
    const raw =
      (client.handshake.auth as { token?: string } | undefined)?.token ??
      (typeof client.handshake.query.token === 'string'
        ? client.handshake.query.token
        : Array.isArray(client.handshake.query.token)
          ? client.handshake.query.token[0]
          : undefined);

    if (!raw?.trim()) {
      this.logger.warn('WS connection rejected: missing token');
      client.disconnect(true);
      return;
    }

    try {
      const payload = this.jwt.verify<JwtPayload>(raw.trim());
      const room = `user:${payload.sub}`;
      await client.join(room);
      client.data.userId = payload.sub;
    } catch (e) {
      this.logger.warn(
        `WS connection rejected: invalid token (${(e as Error).message})`,
      );
      client.disconnect(true);
    }
  }

  emitToUser(userId: string, payload: unknown) {
    this.server.to(`user:${userId}`).emit('notification', payload);
  }
}
