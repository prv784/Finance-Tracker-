package com.financetracker.service.impl;

import com.financetracker.dto.request.CategoryRequest;
import com.financetracker.dto.response.CategoryResponse;
import com.financetracker.entity.Category;
import com.financetracker.entity.User;
import com.financetracker.exception.BadRequestException;
import com.financetracker.exception.ResourceNotFoundException;
import com.financetracker.repository.CategoryRepository;
import com.financetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryResponse createCategory(CategoryRequest request) {
        User user = getCurrentUser();
        if (categoryRepository.existsByNameAndUserId(request.getName(), user.getId())) {
            throw new BadRequestException("Category with name '" + request.getName() + "' already exists");
        }
        Category category = Category.builder()
                .name(request.getName())
                .icon(request.getIcon())
                .color(request.getColor())
                .type(Category.CategoryType.valueOf(request.getType().toUpperCase()))
                .isDefault(false)
                .user(user)
                .build();
        return mapToResponse(categoryRepository.save(category));
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        User user = getCurrentUser();
        Category category = categoryRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        if (category.isDefault()) {
            throw new BadRequestException("Cannot modify default categories");
        }
        category.setName(request.getName());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        category.setType(Category.CategoryType.valueOf(request.getType().toUpperCase()));
        return mapToResponse(categoryRepository.save(category));
    }

    public void deleteCategory(Long id) {
        User user = getCurrentUser();
        Category category = categoryRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        if (category.isDefault()) {
            throw new BadRequestException("Cannot delete default categories");
        }
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories() {
        User user = getCurrentUser();
        return categoryRepository.findByIsDefaultTrueOrUserId(user.getId())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .icon(category.getIcon())
                .color(category.getColor())
                .type(category.getType().name())
                .isDefault(category.isDefault())
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
