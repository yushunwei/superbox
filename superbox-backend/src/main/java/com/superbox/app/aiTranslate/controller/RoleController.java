package com.superbox.app.aiTranslate.controller;

import com.superbox.app.aiTranslate.entity.TranslatorRole;
import com.superbox.app.aiTranslate.service.RoleService;
import com.superbox.common.Result;
import com.superbox.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai-translate/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /** List all translator roles, including both user and system presets. */
    @GetMapping
    public Result<List<TranslatorRole>> list() {
        Long userId = UserContext.getUserId();
        return Result.ok(roleService.list(userId));
    }

    /** Get a single translator role by id. */
    @GetMapping("/{id}")
    public Result<TranslatorRole> get(@PathVariable Long id) {
        return Result.ok(roleService.getById(id));
    }

    /** Create a new translator role. */
    @PostMapping
    public Result<TranslatorRole> create(@RequestBody TranslatorRole role) {
        role.setUserId(UserContext.getUserId());
        return Result.ok(roleService.create(role));
    }

    /** Update an existing translator role. */
    @PutMapping("/{id}")
    public Result<TranslatorRole> update(@PathVariable Long id, @RequestBody TranslatorRole role) {
        return Result.ok(roleService.update(id, role));
    }

    /** Delete a translator role. Only non-preset roles can be deleted. */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }
}
