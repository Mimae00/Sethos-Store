package com.possystem.pos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Grouping used by the terminal to filter the product grid.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_categories_name", columnNames = "name")
)
public class Category extends BaseEntity {

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    /** Hex colour used as the tile accent in the POS grid. */
    @Column(name = "color", length = 16)
    private String color;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    public Category(String name, String description, String color, Integer displayOrder) {
        this.name = name;
        this.description = description;
        this.color = color;
        this.displayOrder = displayOrder == null ? 0 : displayOrder;
    }
}
