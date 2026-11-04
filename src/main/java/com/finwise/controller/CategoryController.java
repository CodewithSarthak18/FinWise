package com.finwise.controller;

import com.finwise.dto.category.CategoryRequest;
import com.finwise.dto.category.CategoryResponse;
import com.finwise.dto.response.ApiResponse;
import com.finwise.entity.CategoryType;
import com.finwise.security.UserPrincipal;
import com.finwise.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories(
            @RequestParam(required = false) CategoryType type,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        List<CategoryResponse> categories;
        if (type != null) {
            log.debug("Fetching categories for user id: {} with type: {}", principal.getId(), type);
            categories = categoryService.getCategoriesByType(principal.getId(), type);
        } else {
            log.debug("Fetching all categories tree for user id: {}", principal.getId());
            categories = categoryService.getCategoriesForUser(principal.getId());
        }
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Creating category '{}' for user id: {}", request.getName(), principal.getId());
        CategoryResponse response = categoryService.createCategory(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Deleting category id: {} for user id: {}", id, principal.getId());
        categoryService.deleteCategory(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully", null));
    }
}
