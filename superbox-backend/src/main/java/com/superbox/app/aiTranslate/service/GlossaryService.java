package com.superbox.app.aiTranslate.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.superbox.app.aiTranslate.entity.GlossaryEntry;
import com.superbox.app.aiTranslate.mapper.GlossaryMapper;
import com.superbox.common.BusinessException;
import com.superbox.common.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlossaryService {

    private final GlossaryMapper glossaryMapper;

    public PageResult<GlossaryEntry> list(Long userId, String sourceLang, String targetLang,
                                           String keyword, int page, int size) {
        int offset = (page - 1) * size;
        if (keyword == null) keyword = "";
        List<GlossaryEntry> records = glossaryMapper.findByUserAndLangs(
                userId, sourceLang, targetLang, keyword, offset, size);
        long total = glossaryMapper.countByUserAndLangs(userId, sourceLang, targetLang, keyword);
        return new PageResult<>(records, total, page, size);
    }

    public GlossaryEntry getById(Long id) {
        GlossaryEntry entry = glossaryMapper.findById(id);
        if (entry == null) {
            throw new BusinessException(404, "术语不存在");
        }
        return entry;
    }

    @Transactional
    public GlossaryEntry create(GlossaryEntry entry) {
        validateEntry(entry);
        glossaryMapper.insert(entry);
        log.info("Created glossary entry id={}, {} -> {}", entry.getId(), entry.getSourceTerm(), entry.getTargetTerm());
        return glossaryMapper.findById(entry.getId());
    }

    @Transactional
    public GlossaryEntry update(Long id, GlossaryEntry entry) {
        GlossaryEntry existing = getById(id);
        if (entry.getSourceTerm() != null) existing.setSourceTerm(entry.getSourceTerm());
        if (entry.getTargetTerm() != null) existing.setTargetTerm(entry.getTargetTerm());
        if (entry.getSourceLang() != null) existing.setSourceLang(entry.getSourceLang());
        if (entry.getTargetLang() != null) existing.setTargetLang(entry.getTargetLang());
        if (entry.getCategory() != null) existing.setCategory(entry.getCategory());
        if (entry.getNote() != null) existing.setNote(entry.getNote());
        validateEntry(existing);
        glossaryMapper.update(existing);
        log.info("Updated glossary entry id={}", id);
        return glossaryMapper.findById(id);
    }

    @Transactional
    public void delete(Long id) {
        getById(id);
        glossaryMapper.delete(id);
        log.info("Deleted glossary entry id={}", id);
    }

    @Transactional
    public int importCsv(Long userId, MultipartFile file, String sourceLang, String targetLang) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }
        try (var reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String[] header = reader.readNext(); // skip header
            List<GlossaryEntry> entries = new ArrayList<>();
            String[] line;
            int lineNum = 1;
            while ((line = reader.readNext()) != null) {
                lineNum++;
                if (line.length < 2) continue;
                GlossaryEntry entry = new GlossaryEntry();
                entry.setUserId(userId);
                entry.setSourceLang(sourceLang);
                entry.setTargetLang(targetLang);
                entry.setSourceTerm(line[0].trim());
                entry.setTargetTerm(line[1].trim());
                entry.setCategory(line.length > 2 && !line[2].isBlank() ? line[2].trim() : null);
                entry.setNote(line.length > 3 && !line[3].isBlank() ? line[3].trim() : null);
                if (entry.getSourceTerm().isEmpty() || entry.getTargetTerm().isEmpty()) {
                    log.warn("Skipping line {}: empty source or target term", lineNum);
                    continue;
                }
                entries.add(entry);
            }
            if (!entries.isEmpty()) {
                glossaryMapper.batchInsert(entries);
                log.info("Imported {} glossary entries for user {}", entries.size(), userId);
            }
            return entries.size();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("CSV import failed", e);
            throw new BusinessException(500, "CSV 导入失败: " + e.getMessage());
        }
    }

    public byte[] exportCsv(Long userId, String sourceLang, String targetLang, String keyword) {
        if (keyword == null) keyword = "";
        List<GlossaryEntry> entries = glossaryMapper.findByUserAndLangs(
                userId, sourceLang, targetLang, keyword, 0, Integer.MAX_VALUE);
        try (var baos = new ByteArrayOutputStream();
             var writer = new CSVWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            writer.writeNext(new String[]{"source_term", "target_term", "category", "note"});
            for (GlossaryEntry entry : entries) {
                writer.writeNext(new String[]{
                        entry.getSourceTerm(),
                        entry.getTargetTerm(),
                        entry.getCategory() != null ? entry.getCategory() : "",
                        entry.getNote() != null ? entry.getNote() : ""
                });
            }
            writer.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("CSV export failed", e);
            throw new BusinessException(500, "CSV 导出失败: " + e.getMessage());
        }
    }

    private void validateEntry(GlossaryEntry entry) {
        if (entry.getSourceTerm() == null || entry.getSourceTerm().isBlank()) {
            throw new BusinessException(400, "源术语不能为空");
        }
        if (entry.getTargetTerm() == null || entry.getTargetTerm().isBlank()) {
            throw new BusinessException(400, "目标术语不能为空");
        }
        if (entry.getSourceTerm().length() > 500) {
            throw new BusinessException(400, "源术语不能超过500个字符");
        }
        if (entry.getTargetTerm().length() > 500) {
            throw new BusinessException(400, "目标术语不能超过500个字符");
        }
    }
}
