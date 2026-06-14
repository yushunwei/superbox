package com.superbox.app.knowledgeBase.controller;

import com.superbox.app.knowledgeBase.entity.KnowledgeFolder;
import com.superbox.app.knowledgeBase.service.KnowledgeFolderService;
import com.superbox.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/knowledge-base/folders")
@RequiredArgsConstructor
public class KnowledgeFolderController {

    private final KnowledgeFolderService folderService;

    @GetMapping
    public Result<List<KnowledgeFolder>> tree() {
        return Result.ok(folderService.tree());
    }

    @GetMapping("/{id}")
    public Result<KnowledgeFolder> getById(@PathVariable Long id) {
        return Result.ok(folderService.getById(id));
    }

    @PostMapping
    public Result<KnowledgeFolder> create(@RequestBody Map<String, Object> body) {
        KnowledgeFolder folder = new KnowledgeFolder();
        folder.setName((String) body.get("name"));
        if (body.get("parentId") != null) {
            folder.setParentId(((Number) body.get("parentId")).longValue());
        }
        if (body.get("sortOrder") != null) {
            folder.setSortOrder(((Number) body.get("sortOrder")).intValue());
        }
        return Result.ok(folderService.create(folder));
    }

    @PutMapping("/{id}")
    public Result<KnowledgeFolder> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        KnowledgeFolder folder = new KnowledgeFolder();
        folder.setName((String) body.get("name"));
        if (body.containsKey("parentId")) {
            folder.setParentId(body.get("parentId") != null ? ((Number) body.get("parentId")).longValue() : null);
        }
        if (body.get("sortOrder") != null) {
            folder.setSortOrder(((Number) body.get("sortOrder")).intValue());
        }
        return Result.ok(folderService.update(id, folder));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        folderService.delete(id);
        return Result.ok();
    }
}
