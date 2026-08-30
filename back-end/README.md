Gateway para Mensagens em Tempo Real

// src/chat/chat.gateway.ts

import { WebSocketGateway, WebSocketServer, SubscribeMessage, MessageBody, ConnectedSocket } from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { ChatService } from './chat.service';

@WebSocketGateway({ cors: { origin: '*' }, namespace: '/ws/chat' })
export class ChatGateway {
  @WebSocketServer()
  server: Server;

  constructor(private readonly chatService: ChatService) {}

  @SubscribeMessage('join_conversation')
  handleJoinRoom(@MessageBody('conversationId') conversationId: string, @ConnectedSocket() client: Socket) {
    client.join(`room_${conversationId}`);
  }

  @SubscribeMessage('send_message')
  async handleSendMessage(
    @MessageBody() data: { conversationId: string; senderId: string; content: string },
  ) {
    const message = await this.chatService.saveMessage(data.conversationId, data.senderId, data.content);
    
    // Transmite a mensagem em tempo real para todos na sala da conversa
    this.server.to(`room_${data.conversationId}`).emit('new_message', message);
  }
}
