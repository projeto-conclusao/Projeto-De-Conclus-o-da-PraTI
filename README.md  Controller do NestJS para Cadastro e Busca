Controller do NestJS para Cadastro e Busca

// src/items/items.controller.ts

import { Controller, Get, Post, Body, Param, Query, UseGuards, Req } from '@nestjs/common';
import { ItemsService } from './items.service';
import { CreateItemDto } from './dto/create-item.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';

@Controller('api/v1/items')
export class ItemsController {
  constructor(private readonly itemsService: ItemsService) {}

  // Cadastro do Objeto (Telas 04, 05, 06)
  @Post()
  @UseGuards(JwtAuthGuard)
  async create(@Req() req, @Body() createItemDto: CreateItemDto) {
    return await this.itemsService.createAndRunAnalysis(req.user.id, createItemDto);
  }

  // Busca e Filtros com Mapa (Tela 02)
  @Get('search')
  async search(
    @Query('type') type?: string,
    @Query('categoryId') categoryId?: string,
    @Query('query') query?: string,
    @Query('lat') lat?: number,
    @Query('lng') lng?: number,
  ) {
    return await this.itemsService.searchItems({ type, categoryId, query, lat, lng });
  }

  // Obter Detalhes (Tela 03)
  @Get(':id')
  async findOne(@Param('id') id: string) {
    return await this.itemsService.findById(id);
  }

  // Obter Possíveis Correspondências (Telas 07, 08, 09)
  @Get(':id/matches')
  @UseGuards(JwtAuthGuard)
  async getMatches(@Param('id') id: string) {
    return await this.itemsService.findItemMatches(id);
  }
}
