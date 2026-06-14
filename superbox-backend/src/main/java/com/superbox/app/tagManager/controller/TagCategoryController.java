package com.superbox.app.tagManager.controller;

import com.superbox.app.tagManager.entity.TagCategory;
import com.superbox.app.tagManager.service.TagCategoryService;
import com.superbox.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tag-categories")
@RequiredArgsConstructor
public class TagCategoryController {

    private final TagCategoryService service;

    @GetMapping
    public Result<List<TagCategory>> list() {
        return Result.ok(service.list());
    }

    @GetMapping("/{id}")
    public Result<TagCategory> get(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @PostMapping
    public Result<TagCategory> create(@RequestBody TagCategory category) {
        return Result.ok(service.create(category));
    }

    @PutMapping("/{id}")
    public Result<TagCategory> update(@PathVariable Long id, @RequestBody TagCategory category) {
        return Result.ok(service.update(id, category));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
