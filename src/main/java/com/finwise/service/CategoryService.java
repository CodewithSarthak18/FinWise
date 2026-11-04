package com.finwise.service;

import com.finwise.dto.category.CategoryRequest;
import com.finwise.dto.category.CategoryResponse;
import com.finwise.entity.CategoryType;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getCategoriesForUser(Long userId);

    List<CategoryResponse> getCategoriesByType(Long userId, CategoryType type);

    CategoryResponse createCategory(CategoryRequest request, Long userId);

    void deleteCategory(Long categoryId, Long userId);
}
