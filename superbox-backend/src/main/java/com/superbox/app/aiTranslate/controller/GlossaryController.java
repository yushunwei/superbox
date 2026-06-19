package com.superbox.app.aiTranslate.controller;

import com.superbox.app.aiTranslate.entity.GlossaryEntry;
import com.superbox.app.aiTranslate.service.GlossaryService;
import com.superbox.common.PageResult;
import com.superbox.common.Result;
import com.superbox.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ai-translate/glossary")
@RequiredArgsConstructor
public class GlossaryController {

    private final GlossaryService glossaryService;

    /** List glossary entries with pagination, language filter, and keyword search. */
    @GetMapping
    public Result<PageResult<GlossaryEntry>> list(
            @RequestParam(required = false) String sourceLang,
            @RequestParam(required = false) String targetLang,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long userId = UserContext.getUserId();
        return Result.ok(glossaryService.list(userId, sourceLang, targetLang, keyword, page, size));
    }

    /** Create a new glossary entry. */
    @PostMapping
    public Result<GlossaryEntry> create(@RequestBody GlossaryEntry entry) {
        entry.setUserId(UserContext.getUserId());
        return Result.ok(glossaryService.create(entry));
    }

    /** Import glossary entries from CSV file. */
    @PostMapping("/import")
    public Result<Integer> importCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String sourceLang,
            @RequestParam(required = false) String targetLang) {
        Long userId = UserContext.getUserId();
        int count = glossaryService.importCsv(userId, file, sourceLang, targetLang);
        return Result.ok(count);
    }

    /** Export glossary entries as CSV file download. */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String sourceLang,
            @RequestParam(required = false) String targetLang,
            @RequestParam(required = false) String keyword) {
        Long userId = UserContext.getUserId();
        byte[] csvBytes = glossaryService.exportCsv(userId, sourceLang, targetLang, keyword);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"glossary_export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvBytes);
    }

    /** Update a glossary entry. */
    @PutMapping("/{id}")
    public Result<GlossaryEntry> update(@PathVariable Long id, @RequestBody GlossaryEntry entry) {
        return Result.ok(glossaryService.update(id, entry));
    }

    /** Delete a glossary entry. */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        glossaryService.delete(id);
        return Result.ok();
    }
}
