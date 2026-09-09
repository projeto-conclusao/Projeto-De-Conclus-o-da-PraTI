package com.achadosedevolvidos.item.repository;

import com.achadosedevolvidos.item.dto.ItemSearchFilter;
import com.achadosedevolvidos.item.model.Item;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * Mantém a lógica de filtros fora do Controller e do Service: cada filtro é uma
 * Specification isolada, testável separadamente e combinável com "and".
 *
 * <p>lat/lng ainda não são usados aqui: uma busca por raio geográfico correta
 * precisaria de PostGIS (ou de uma expressão Haversine em SQL nativo), o que foi
 * deixado como próximo passo em vez de uma implementação aproximada e incorreta.</p>
 */
public final class ItemSpecifications {

    private ItemSpecifications() {}

    public static Specification<Item> withFilters(ItemSearchFilter filter) {
        return Specification
                .where(hasType(filter.type()))
                .and(hasCategory(filter.categoryId()))
                .and(matchesQuery(filter.query()));
    }

    public static Specification<Item> hasType(Item.ItemType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Item> hasCategory(UUID categoryId) {
        return (root, query, cb) -> categoryId == null
                ? null
                : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Item> matchesQuery(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return null;
            }
            String like = "%" + text.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("description")), like)
            );
        };
    }
}
