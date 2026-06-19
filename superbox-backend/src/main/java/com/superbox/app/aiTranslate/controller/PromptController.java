package com.superbox.app.aiTranslate.controller;

import com.superbox.app.aiTranslate.entity.PromptTemplate;
import com.superbox.app.aiTranslate.service.PromptService;
import com.superbox.common.Result;
import com.superbox.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai-translate/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;

    /** List prompt templates, optionally filtered by category. Includes both user and system presets. */
    @GetMapping
    public Result<List<PromptTemplate>> list(
            @RequestParam(required = false) String category) {
        Long userId = UserContext.getUserId();
        return Result.ok(promptService.list(userId, category));
    }

    /** Get a single prompt template by id. */
    @GetMapping("/{id}")
    public Result<PromptTemplate> get(@PathVariable Long id) {
        return Result.ok(promptService.getById(id));
    }

    /** Create a new prompt template. */
    @PostMapping
    public Result<PromptTemplate> create(@RequestBody PromptTemplate template) {
        template.setUserId(UserContext.getUserId());
        return Result.ok(promptService.create(template));
    }

    /** Update an existing prompt template. */
    @PutMapping("/{id}")
    public Result<PromptTemplate> update(@PathVariable Long id, @RequestBody PromptTemplate template) {
        return Result.ok(promptService.update(id, template));
    }

    /** Delete a prompt template. Only non-preset templates can be deleted. */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        promptService.delete(id);
        return Result.ok();
    }
}
