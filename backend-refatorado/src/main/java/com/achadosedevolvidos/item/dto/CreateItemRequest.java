package com.achadosedevolvidos.item.dto;

import com.achadosedevolvidos.item.model.Item;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateItemRequest(

        @NotNull(message = "Tipo é obrigatório (PERDIDO ou ENCONTRADO)")
        Item.ItemType type,

        @NotNull(message = "Categoria é obrigatória")
        UUID categoryId,

        @NotBlank(message = "Título é obrigatório")
        String title,

        String description,

        String locationText,

        @NotNull(message = "Latitude é obrigatória")
        Double latitude,

        @NotNull(message = "Longitude é obrigatória")
        Double longitude,

        @NotNull(message = "Data do evento é obrigatória")
        @PastOrPresent(message = "Data do evento não pode estar no futuro")
        LocalDateTime eventDate,

        List<String> imageUrls
) {}
