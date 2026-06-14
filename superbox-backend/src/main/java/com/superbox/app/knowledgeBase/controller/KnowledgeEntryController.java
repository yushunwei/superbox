package com.superbox.app.knowledgeBase.controller;

import com.superbox.app.knowledgeBase.entity.KnowledgeEntry;
import com.superbox.app.knowledgeBase.service.KnowledgeEntryService;
import com.superbox.common.PageResult;
import com.superbox.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/knowledge-base")
@RequiredArgsConstructor
public class KnowledgeEntryController {

    private final KnowledgeEntryService entryService;

    @GetMapping
    public Result<PageResult<KnowledgeEntry>> list(
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(entryService.list(folderId, keyword, page, size));
    }

    @GetMapping("/{id}")
    public Result<KnowledgeEntry> getById(@PathVariable Long id) {
        return Result.ok(entryService.getById(id));
    }

    @PostMapping("/manual")
    public Result<KnowledgeEntry> createManual(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        Long folderId = body.get("folderId") != null ? ((Number) body.get("folderId")).longValue() : null;
        @SuppressWarnings("unchecked")
        List<Integer> rawLabelIds = (List<Integer>) body.get("labelIds");
        List<Long> labelIds = rawLabelIds != null ? rawLabelIds.stream().map(Integer::longValue).toList() : null;
        return Result.ok(entryService.createManual(title, content, folderId, labelIds));
    }

    @PostMapping("/upload")
    public Result<KnowledgeEntry> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long folderId) {
        return Result.ok(entryService.createFromFile(file, folderId, null));
    }

    @PutMapping("/{id}")
    public Result<KnowledgeEntry> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        Long folderId = body.get("folderId") != null ? ((Number) body.get("folderId")).longValue() : null;
        @SuppressWarnings("unchecked")
        List<Integer> rawLabelIds = (List<Integer>) body.get("labelIds");
        List<Long> labelIds = rawLabelIds != null ? rawLabelIds.stream().map(Integer::longValue).toList() : null;
        return Result.ok(entryService.update(id, title, content, folderId, labelIds));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        entryService.delete(id);
        return Result.ok();
    }
}
