package com.achadosedevolvidos.match.dto;

import com.achadosedevolvidos.item.model.Item;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa só o que o algoritmo de match precisa saber sobre um item.
 * Isso desacopla {@code MatchEngineService} inteiramente do JPA: o motor não
 * carrega, não faz lazy-loading e não precisa de um EntityManager/contexto do
 * Hibernate para funcionar — pode ser testado com um simples "new".
 */
public record MatchCandidate(
        UUID itemId,
        Item.ItemType type,
        UUID categoryId,
        Double latitude,
        Double longitude,
        LocalDateTime eventDate,
        String title
) {}
