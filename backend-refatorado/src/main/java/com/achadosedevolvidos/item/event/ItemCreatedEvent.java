package com.achadosedevolvidos.item.event;

import java.util.UUID;

/**
 * Evento de domínio publicado após a criação (e commit) de um item. Quem reage a
 * ele — hoje, o motor de match — vive em outro módulo e não é conhecido pelo
 * ItemService, que apenas publica o evento via ApplicationEventPublisher.
 */
public record ItemCreatedEvent(UUID itemId) {}
