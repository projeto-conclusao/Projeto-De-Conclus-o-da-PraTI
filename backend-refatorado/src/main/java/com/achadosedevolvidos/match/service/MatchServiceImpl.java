package com.achadosedevolvidos.match.service;

import com.achadosedevolvidos.item.model.Item;
import com.achadosedevolvidos.item.repository.ItemRepository;
import com.achadosedevolvidos.match.dto.MatchCandidate;
import com.achadosedevolvidos.match.dto.MatchResponse;
import com.achadosedevolvidos.match.mapper.MatchMapper;
import com.achadosedevolvidos.match.model.Match;
import com.achadosedevolvidos.match.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * É esta camada — não o {@link MatchEngineService} — que sabe de JPA/persistência.
 * O motor de pontuação em si permanece puro; aqui só se faz a ponte entre entidade
 * e {@link MatchCandidate}, e a decisão de o que persistir.
 */
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    /** Score mínimo (0–100) para um match ser considerado relevante o bastante para persistir. */
    private static final double MIN_SCORE_TO_PERSIST = 40.0;

    private final ItemRepository itemRepository;
    private final MatchRepository matchRepository;
    private final MatchEngineService matchEngineService;
    private final MatchMapper matchMapper;

    @Override
    @Transactional
    public void analyzeNewItem(Item newItem) {
        Item.ItemType oppositeType = newItem.getType() == Item.ItemType.PERDIDO
                ? Item.ItemType.ENCONTRADO
                : Item.ItemType.PERDIDO;

        List<Item> candidates = itemRepository.findByTypeAndCategoryIdAndStatusNot(
                oppositeType, newItem.getCategory().getId(), Item.ItemStatus.INATIVO
        );

        MatchCandidate newItemCandidate = toCandidate(newItem);

        for (Item candidate : candidates) {
            double score = matchEngineService.calculateMatchScore(newItemCandidate, toCandidate(candidate));

            if (score >= MIN_SCORE_TO_PERSIST) {
                persistMatch(newItem, candidate, score);
            }
        }
    }

    @Override
    public List<MatchResponse> findMatchesForItem(UUID itemId) {
        return matchRepository.findByLostItemIdOrFoundItemId(itemId, itemId)
                .stream()
                .map(matchMapper::toResponse)
                .toList();
    }

    private void persistMatch(Item newItem, Item candidate, double score) {
        Item lostItem = newItem.getType() == Item.ItemType.PERDIDO ? newItem : candidate;
        Item foundItem = newItem.getType() == Item.ItemType.ENCONTRADO ? newItem : candidate;

        Match match = Match.builder()
                .lostItem(lostItem)
                .foundItem(foundItem)
                .score(score)
                .build();

        matchRepository.save(match);
    }

    private MatchCandidate toCandidate(Item item) {
        return new MatchCandidate(
                item.getId(),
                item.getType(),
                item.getCategory().getId(),
                item.getLatitude(),
                item.getLongitude(),
                item.getEventDate(),
                item.getTitle()
        );
    }
}
