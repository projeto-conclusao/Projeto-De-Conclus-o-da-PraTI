package com.achadosedevolvidos.category.controller;

import com.achadosedevolvidos.category.dto.CategoryResponse;
import com.achadosedevolvidos.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint público e somente leitura — não há regra de negócio além de listar,
 * por isso acessa o repositório diretamente, sem uma camada de Service dedicada.
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> findAll() {
        List<CategoryResponse> categories = categoryRepository.findAll().stream()
                .map(category -> new CategoryResponse(category.getId(), category.getName(), category.getIconUrl()))
                .toList();
        return ResponseEntity.ok(categories);
    }
}
