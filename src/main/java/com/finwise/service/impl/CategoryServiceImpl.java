package com.finwise.service.impl;

import com.finwise.dto.category.CategoryRequest;
import com.finwise.dto.category.CategoryResponse;
import com.finwise.entity.Category;
import com.finwise.entity.CategoryType;
import com.finwise.entity.User;
import com.finwise.exception.BadRequestException;
import com.finwise.exception.ResourceNotFoundException;
import com.finwise.exception.UnauthorizedException;
import com.finwise.mapper.CategoryMapper;
import com.finwise.repository.CategoryRepository;
import com.finwise.repository.UserRepository;
import com.finwise.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesForUser(Long userId) {
        return categoryRepository.findAllAvailableForUser(userId).stream()
                .filter(c -> c.getParent() == null) // Root categories
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesByType(Long userId, CategoryType type) {
        return categoryRepository.findAvailableForUserByType(userId, type).stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findByIdAndDeletedAtIsNull(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Category", "id", request.getParentId()));

            if (!parent.isSystem() && (parent.getUser() == null || !parent.getUser().getId().equals(userId))) {
                throw new UnauthorizedException("You do not have access to parent category with id: " + request.getParentId());
            }
        }

        Category category = categoryMapper.toEntity(request, user, parent);
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId, Long userId) {
        Category category = categoryRepository.findByIdAndDeletedAtIsNull(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

        if (category.isSystem()) {
            throw new BadRequestException("System default categories cannot be deleted");
        }

        if (category.getUser() == null || !category.getUser().getId().equals(userId)) {
            throw new BadRequestException("You do not have permission to delete this category");
        }

        category.setDeletedAt(Instant.now());
        categoryRepository.save(category);
    }
}
