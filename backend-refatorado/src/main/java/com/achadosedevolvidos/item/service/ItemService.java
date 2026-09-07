package com.achadosedevolvidos.item.service;

import com.achadosedevolvidos.item.dto.CreateItemRequest;
import com.achadosedevolvidos.item.dto.ItemResponse;
import com.achadosedevolvidos.item.dto.ItemSearchFilter;

import java.util.List;
import java.util.UUID;

public interface ItemService {

    ItemResponse createAndAnalyze(UUID currentUserId, CreateItemRequest request);

    List<ItemResponse> search(ItemSearchFilter filter);

    ItemResponse findById(UUID id);
}
