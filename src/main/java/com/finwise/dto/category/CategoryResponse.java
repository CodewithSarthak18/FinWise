package com.finwise.dto.category;

import com.finwise.entity.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private Long id;
    private String name;
    private CategoryType type;
    private Long parentId;
    private String icon;
    private String colorCode;
    private boolean system;
    private List<CategoryResponse> subcategories;
    private Instant createdAt;
}
