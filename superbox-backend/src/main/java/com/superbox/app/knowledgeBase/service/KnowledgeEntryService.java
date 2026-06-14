package com.superbox.app.knowledgeBase.service;

import com.superbox.app.knowledgeBase.entity.KnowledgeChunk;
import com.superbox.app.knowledgeBase.entity.KnowledgeEntry;
import com.superbox.app.knowledgeBase.mapper.KnowledgeChunkMapper;
import com.superbox.app.knowledgeBase.mapper.KnowledgeEntryMapper;
import com.superbox.app.knowledgeBase.mapper.KnowledgeFolderMapper;
import com.superbox.common.BusinessException;
import com.superbox.common.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeEntryService {

    private final KnowledgeEntryMapper entryMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final KnowledgeFolderMapper folderMapper;
    private final FileParsingService fileParsingService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;

    @Value("${superbox.storage.root-path:./storage}")
    private String storageRoot;

    public PageResult<KnowledgeEntry> list(Long folderId, String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<KnowledgeEntry> entries = entryMapper.list(folderId, keyword, offset, size);
        for (KnowledgeEntry entry : entries) {
            entry.setTags(entryMapper.findTagsByEntryId(entry.getId()));
        }
        long total = entryMapper.count(folderId, keyword);
        return new PageResult<>(entries, total, page, size);
    }

    public KnowledgeEntry getById(Long id) {
        KnowledgeEntry entry = entryMapper.findById(id);
        if (entry == null) throw new BusinessException(404, "知识条目不存在");
        entry.setTags(entryMapper.findTagsByEntryId(id));
        entry.setChunks(chunkMapper.findByEntryId(id));
        return entry;
    }

    @Transactional
    public KnowledgeEntry createManual(String title, String content, Long folderId, List<Long> tagIds) {
        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setTitle(title);
        entry.setContentText(content);
        entry.setFolderId(folderId);
        entry.setSourceType("manual");
        entry.setChunkCount(0);
        entryMapper.insert(entry);

        if (tagIds != null) {
            for (Long tagId : tagIds) tagEntry(entry.getId(), tagId);
        }
        if (folderId != null) folderMapper.updateEntryCount(folderId, 1);

        processChunks(entry, content);
        return getById(entry.getId());
    }

    @Transactional
    public KnowledgeEntry createFromFile(MultipartFile file, Long folderId, List<Long> tagIds) {
        String originalName = file.getOriginalFilename();
        String content = fileParsingService.parse(file);
        String filePath = storeFile(file);

        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setTitle(originalName != null ? originalName : "未命名文件");
        entry.setContentText(content);
        entry.setFolderId(folderId);
        entry.setSourceType("file_upload");
        entry.setFilePath(filePath);
        entry.setFileType(getExtension(originalName));
        entry.setFileSize(file.getSize());
        entry.setChunkCount(0);
        entryMapper.insert(entry);

        if (tagIds != null) {
            for (Long tagId : tagIds) tagEntry(entry.getId(), tagId);
        }
        if (folderId != null) folderMapper.updateEntryCount(folderId, 1);

        processChunks(entry, content);
        return getById(entry.getId());
    }

    @Transactional
    public KnowledgeEntry update(Long id, String title, String content, Long folderId, List<Long> tagIds) {
        KnowledgeEntry entry = getById(id);
        Long oldFolderId = entry.getFolderId();

        entry.setTitle(title);
        entry.setContentText(content);
        entry.setFolderId(folderId);
        entryMapper.update(entry);

        if (tagIds != null) {
            entryMapper.deleteTags(id);
            for (Long tagId : tagIds) tagEntry(id, tagId);
        }

        if (oldFolderId != null) folderMapper.updateEntryCount(oldFolderId, -1);
        if (folderId != null) folderMapper.updateEntryCount(folderId, 1);

        if (content != null) {
            chunkMapper.deleteByEntryId(id);
            processChunks(entry, content);
        }
        return getById(id);
    }

    @Transactional
    public void delete(Long id) {
        KnowledgeEntry entry = getById(id);
        if (entry.getFolderId() != null) folderMapper.updateEntryCount(entry.getFolderId(), -1);
        chunkMapper.deleteByEntryId(id);
        entryMapper.delete(id);
    }

    private void processChunks(KnowledgeEntry entry, String content) {
        if (content == null || content.isBlank()) return;
        List<String> chunks = chunkingService.chunk(content);
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setEntryId(entry.getId());
            chunk.setChunkIndex(i);
            chunk.setContent(chunks.get(i));
            chunk.setTokenCount(chunkingService.estimateTokens(chunks.get(i)));
            chunkMapper.insert(chunk);
        }
        entryMapper.updateChunkCount(entry.getId(), chunks.size());
        log.info("Processed {} chunks for entry {}", chunks.size(), entry.getId());
    }

    private void tagEntry(Long entryId, Long tagId) {
        entryMapper.insertTag(entryId, tagId);
    }

    private String storeFile(MultipartFile file) {
        try {
            String datePath = LocalDate.now().toString().replace("-", "/");
            Path dir = Paths.get(storageRoot, "files", datePath);
            Files.createDirectories(dir);
            String storedName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path target = dir.resolve(storedName);
            file.transferTo(target);
            return target.toString().replace('\\', '/');
        } catch (IOException e) {
            throw new BusinessException(500, "文件存储失败: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i + 1).toLowerCase() : "";
    }
}
