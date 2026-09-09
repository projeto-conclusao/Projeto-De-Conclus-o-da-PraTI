package com.achadosedevolvidos.match.listener;

import com.achadosedevolvidos.item.event.ItemCreatedEvent;
import com.achadosedevolvidos.item.repository.ItemRepository;
import com.achadosedevolvidos.match.service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Só existe porque o ItemService publicou um ItemCreatedEvent — o módulo item
 * nunca importa nada daqui. AFTER_COMMIT garante que só rodamos a análise se a
 * criação do item realmente foi persistida; @Async garante que uma eventual
 * lentidão (ou falha) do motor de match nunca afeta a resposta HTTP de quem
 * criou o item.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ItemCreatedEventListener {

    private final ItemRepository itemRepository;
    private final MatchService matchService;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onItemCreated(ItemCreatedEvent event) {
        try {
            itemRepository.findById(event.itemId()).ifPresent(matchService::analyzeNewItem);
        } catch (Exception e) {
            log.error("Falha ao analisar matches para o item {}", event.itemId(), e);
        }
    }
}
