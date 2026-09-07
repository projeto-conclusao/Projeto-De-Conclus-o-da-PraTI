package com.achadosedevolvidos.item.controller;

import com.achadosedevolvidos.item.dto.CreateItemRequest;
import com.achadosedevolvidos.item.dto.ItemResponse;
import com.achadosedevolvidos.item.dto.ItemSearchFilter;
import com.achadosedevolvidos.item.model.Item;
import com.achadosedevolvidos.item.service.ItemService;
import com.achadosedevolvidos.match.dto.MatchResponse;
import com.achadosedevolvidos.match.service.MatchService;
import com.achadosedevolvidos.user.model.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Substitui os dois Controllers conflitantes do repositório original (um em
 * com.achadosedevolvidos.controller, outro em com.example.api.controller, ambos
 * mapeando /api/v1/items). O usuário autenticado agora vem de
 * {@code @AuthenticationPrincipal}, que funciona tanto para quem logou via Bearer
 * JWT quanto via OAuth2/Google — o Controller não sabe nem precisa saber qual dos
 * dois foi usado. Antes, {@code create()} lia um {@code @RequestAttribute("userId")}
 * que nenhum filtro no projeto chegava a preencher.
 */
@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final MatchService matchService;

    @PostMapping
    public ResponseEntity<ItemResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CreateItemRequest request
    ) {
        ItemResponse created = itemService.createAndAnalyze(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ItemResponse>> search(
            @RequestParam(required = false) Item.ItemType type,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng
    ) {
        ItemSearchFilter filter = new ItemSearchFilter(type, categoryId, query, lat, lng);
        return ResponseEntity.ok(itemService.search(filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(itemService.findById(id));
    }

    @GetMapping("/{id}/matches")
    public ResponseEntity<List<MatchResponse>> getMatches(@PathVariable UUID id) {
        return ResponseEntity.ok(matchService.findMatchesForItem(id));
    }
}
