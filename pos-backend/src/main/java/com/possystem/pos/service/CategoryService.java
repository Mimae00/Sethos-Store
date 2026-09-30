package com.possystem.pos.service;

import com.possystem.pos.domain.Category;
import com.possystem.pos.dto.CategoryRequest;
import com.possystem.pos.dto.CategoryResponse;
import com.possystem.pos.exception.BusinessRuleException;
import com.possystem.pos.exception.DuplicateResourceException;
import com.possystem.pos.exception.ResourceNotFoundException;
import com.possystem.pos.repository.CategoryRepository;
import com.possystem.pos.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categories;
    private final ProductRepository products;

    public CategoryService(CategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    public List<CategoryResponse> findAll() {
        return categories.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse findById(Long id) {
        return CategoryResponse.from(require(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categories.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A category named '" + name + "' already exists");
        }
        Category category = new Category();
        apply(category, request, name);
        return CategoryResponse.from(categories.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = require(id);
        String name = request.name().trim();
        categories.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("A category named '" + name + "' already exists");
            }
        });
        apply(category, request, name);
        return CategoryResponse.from(categories.save(category));
    }

    /**
     * Refuses to delete a category that still has products, rather than silently
     * orphaning them. The client is expected to reassign first.
     */
    @Transactional
    public void delete(Long id) {
        Category category = require(id);
        if (products.existsByCategoryId(id)) {
            throw new BusinessRuleException(
                    "'" + category.getName() + "' still has products. Move or delete them first.");
        }
        categories.delete(category);
    }

    private void apply(Category category, CategoryRequest request, String name) {
        category.setName(name);
        category.setDescription(trimToNull(request.description()));
        category.setColor(trimToNull(request.color()));
        category.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
    }

    private Category require(Long id) {
        return categories.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Category", id));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
