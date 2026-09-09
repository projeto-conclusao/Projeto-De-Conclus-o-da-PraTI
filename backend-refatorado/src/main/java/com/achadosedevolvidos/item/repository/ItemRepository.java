package com.achadosedevolvidos.item.repository;

import com.achadosedevolvidos.item.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID>, JpaSpecificationExecutor<Item> {

    /**
     * Usado pelo motor de match para achar candidatos do tipo oposto na mesma
     * categoria, excluindo itens já inativos.
     */
    List<Item> findByTypeAndCategoryIdAndStatusNot(
            Item.ItemType type, UUID categoryId, Item.ItemStatus excludedStatus
    );
}
