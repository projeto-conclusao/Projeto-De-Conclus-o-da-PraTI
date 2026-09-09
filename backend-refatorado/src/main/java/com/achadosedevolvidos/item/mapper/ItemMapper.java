package com.achadosedevolvidos.item.mapper;

import com.achadosedevolvidos.item.dto.ItemImageResponse;
import com.achadosedevolvidos.item.dto.ItemResponse;
import com.achadosedevolvidos.item.model.Item;
import com.achadosedevolvidos.item.model.ItemImage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ItemMapper {

    public ItemResponse toResponse(Item item) {
        List<ItemImageResponse> images = item.getImages() == null
                ? List.of()
                : item.getImages().stream().map(this::toImageResponse).toList();

        return new ItemResponse(
                item.getId(),
                item.getUser().getId(),
                item.getCategory().getId(),
                item.getCategory().getName(),
                item.getType(),
                item.getStatus(),
                item.getTitle(),
                item.getDescription(),
                item.getLocationText(),
                item.getLatitude(),
                item.getLongitude(),
                item.getEventDate(),
                item.getCreatedAt(),
                images
        );
    }

    public ItemImageResponse toImageResponse(ItemImage image) {
        return new ItemImageResponse(image.getId(), image.getUrl(), image.getDisplayOrder());
    }
}
