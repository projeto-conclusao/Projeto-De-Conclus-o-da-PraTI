package com.achadosedevolvidos.match.service;

import com.achadosedevolvidos.item.model.Item;
import com.achadosedevolvidos.match.dto.MatchResponse;

import java.util.List;
import java.util.UUID;

public interface MatchService {

    /**
     * Compara o item recém-criado com os candidatos do tipo oposto na mesma
     * categoria e persiste os matches cujo score ultrapasse o limite mínimo.
     */
    void analyzeNewItem(Item newItem);

    List<MatchResponse> findMatchesForItem(UUID itemId);
}
