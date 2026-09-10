package com.achadosedevolvidos.item.service;

import com.achadosedevolvidos.category.model.Category;
import com.achadosedevolvidos.category.repository.CategoryRepository;
import com.achadosedevolvidos.item.dto.CreateItemRequest;
import com.achadosedevolvidos.item.dto.ItemResponse;
import com.achadosedevolvidos.item.dto.ItemSearchFilter;
import com.achadosedevolvidos.item.event.ItemCreatedEvent;
import com.achadosedevolvidos.item.mapper.ItemMapper;
import com.achadosedevolvidos.item.model.Item;
import com.achadosedevolvidos.item.model.ItemImage;
import com.achadosedevolvidos.item.repository.ItemRepository;
import com.achadosedevolvidos.item.repository.ItemSpecifications;
import com.achadosedevolvidos.shared.exception.AppException;
import com.achadosedevolvidos.user.model.User;
import com.achadosedevolvidos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Propositalmente NÃO depende de MatchService (nem de nada do módulo match):
 * ao criar um item, só publica {@link ItemCreatedEvent} e segue em frente. Quem
 * reage a esse evento (o motor de match) é decidido em outro módulo, de forma
 * assíncrona e após o commit da transação — ver
 * {@code match.listener.ItemCreatedEventListener}.
 */
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ItemMapper itemMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ItemResponse createAndAnalyze(UUID currentUserId, CreateItemRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException("Usuário não encontrado", HttpStatus.UNAUTHORIZED));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new AppException("Categoria não encontrada", HttpStatus.BAD_REQUEST));

        Item item = Item.builder()
                .user(user)
                .category(category)
                .type(request.type())
                .title(request.title())
                .description(request.description())
                .shortDescription(request.shortDescription())
                .locationText(request.locationText())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .eventDate(request.eventDate())
                .build();

        item.setImages(buildImages(item, request.imageUrls()));

        Item saved = itemRepository.save(item);

        eventPublisher.publishEvent(new ItemCreatedEvent(saved.getId()));

        return itemMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemResponse> search(ItemSearchFilter filter) {
        return itemRepository.findAll(ItemSpecifications.withFilters(filter))
                .stream()
                .map(itemMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ItemResponse findById(UUID id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new AppException("Item não encontrado", HttpStatus.NOT_FOUND));
        return itemMapper.toResponse(item);
    }

    private List<ItemImage> buildImages(Item item, List<String> imageUrls) {
        List<ItemImage> images = new ArrayList<>();
        if (imageUrls == null) {
            return images;
        }
        int order = 0;
        for (String url : imageUrls) {
            images.add(ItemImage.builder().item(item).url(url).displayOrder(order++).build());
        }
        return images;
    }
}
