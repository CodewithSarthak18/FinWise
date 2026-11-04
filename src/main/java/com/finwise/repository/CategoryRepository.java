package com.finwise.repository;

import com.finwise.entity.Category;
import com.finwise.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE (c.user.id = :userId OR c.system = true) AND c.deletedAt IS NULL ORDER BY c.name ASC")
    List<Category> findAllAvailableForUser(@Param("userId") Long userId);

    @Query("SELECT c FROM Category c WHERE (c.user.id = :userId OR c.system = true) AND c.type = :type AND c.deletedAt IS NULL ORDER BY c.name ASC")
    List<Category> findAvailableForUserByType(@Param("userId") Long userId, @Param("type") CategoryType type);

    Optional<Category> findByIdAndDeletedAtIsNull(Long id);
}
