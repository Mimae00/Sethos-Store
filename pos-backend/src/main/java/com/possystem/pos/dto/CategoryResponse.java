package com.possystem.pos.dto;

import com.possystem.pos.domain.Category;

public record CategoryResponse(
        Long id,
        String name,
        String description,
        String color,
        Integer displayOrder
) {

    public static CategoryResponse from(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getColor(),
                category.getDisplayOrder()
        );
    }
}
