package com.achadosedevolvidos.item.dto;

import com.achadosedevolvidos.item.model.Item;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ItemResponse(
        UUID id,
        UUID userId,
        UUID categoryId,
        String categoryName,
        String categoryIconUrl,
        Item.ItemType type,
        Item.ItemStatus status,
        String title,
        String description,
        String shortDescription,
        String locationText,
        Double latitude,
        Double longitude,
        LocalDateTime eventDate,
        LocalDateTime createdAt,
        List<ItemImageResponse> images
) {}
