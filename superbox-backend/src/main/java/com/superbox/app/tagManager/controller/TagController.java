package com.superbox.app.tagManager.controller;

import com.superbox.app.tagManager.entity.Tag;
import com.superbox.app.tagManager.service.TagService;
import com.superbox.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public Result<List<Tag>> list(@RequestParam(required = false) Long categoryId) {
        return Result.ok(tagService.list(categoryId));
    }

    @GetMapping("/{id}")
    public Result<Tag> get(@PathVariable Long id) {
        return Result.ok(tagService.getById(id));
    }

    @PostMapping
    public Result<Tag> create(@RequestBody Tag tag) {
        return Result.ok(tagService.create(tag));
    }

    @PutMapping("/{id}")
    public Result<Tag> update(@PathVariable Long id, @RequestBody Tag tag) {
        return Result.ok(tagService.update(id, tag));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.ok();
    }
}
