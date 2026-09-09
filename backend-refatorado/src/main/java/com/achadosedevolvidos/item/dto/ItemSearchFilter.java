package com.achadosedevolvidos.item.dto;

import com.achadosedevolvidos.item.model.Item;

import java.util.UUID;

/**
 * lat/lng ficam reservados para uma futura busca por proximidade (hoje não
 * aplicada na consulta — ver observação em {@code ItemSpecifications}).
 */
public record ItemSearchFilter(
        Item.ItemType type,
        UUID categoryId,
        String query,
        Double latitude,
        Double longitude
) {}
