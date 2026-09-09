package com.achadosedevolvidos.category.repository;

import com.achadosedevolvidos.category.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
}
