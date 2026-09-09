package com.achadosedevolvidos.item.repository;

import com.achadosedevolvidos.item.model.ItemImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ItemImageRepository extends JpaRepository<ItemImage, UUID> {
}
