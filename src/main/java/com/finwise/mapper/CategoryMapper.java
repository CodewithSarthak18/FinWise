package com.finwise.mapper;

import com.finwise.dto.category.CategoryRequest;
import com.finwise.dto.category.CategoryResponse;
import com.finwise.entity.Category;
import com.finwise.entity.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CategoryMapper {

    public Category toEntity(CategoryRequest request, User user, Category parent) {
        if (request == null) {
            return null;
        }

        return Category.builder()
                .user(user)
                .parent(parent)
                .name(request.getName())
                .type(request.getType())
                .icon(request.getIcon() != null ? request.getIcon() : "tag")
                .colorCode(request.getColorCode() != null ? request.getColorCode() : "#4F46E5")
                .system(false)
                .build();
    }

    public CategoryResponse toResponse(Category category) {
        if (category == null) {
            return null;
        }

        List<CategoryResponse> subcategoryResponses = Collections.emptyList();
        if (category.getSubcategories() != null && !category.getSubcategories().isEmpty()) {
            subcategoryResponses = category.getSubcategories().stream()
                    .filter(sub -> !sub.isDeleted())
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .icon(category.getIcon())
                .colorCode(category.getColorCode())
                .system(category.isSystem())
                .subcategories(subcategoryResponses)
                .createdAt(category.getCreatedAt())
                .build();
    }
}
