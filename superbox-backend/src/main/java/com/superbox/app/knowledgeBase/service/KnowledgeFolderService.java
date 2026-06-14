package com.superbox.app.knowledgeBase.service;

import com.superbox.app.knowledgeBase.entity.KnowledgeFolder;
import com.superbox.app.knowledgeBase.mapper.KnowledgeFolderMapper;
import com.superbox.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeFolderService {

    private final KnowledgeFolderMapper folderMapper;

    public List<KnowledgeFolder> tree() {
        List<KnowledgeFolder> all = folderMapper.findAll();
        Map<Long, List<KnowledgeFolder>> byParent = all.stream()
                .filter(f -> f.getParentId() != null)
                .collect(Collectors.groupingBy(KnowledgeFolder::getParentId));

        List<KnowledgeFolder> roots = new ArrayList<>();
        for (KnowledgeFolder f : all) {
            f.setChildren(byParent.getOrDefault(f.getId(), List.of()));
            if (f.getParentId() == null) {
                roots.add(f);
            }
        }
        return roots;
    }

    public KnowledgeFolder getById(Long id) {
        KnowledgeFolder folder = folderMapper.findById(id);
        if (folder == null) throw new BusinessException(404, "目录不存在");
        return folder;
    }

    @Transactional
    public KnowledgeFolder create(KnowledgeFolder folder) {
        if (folder.getSortOrder() == null) folder.setSortOrder(0);
        folderMapper.insert(folder);
        return folder;
    }

    @Transactional
    public KnowledgeFolder update(Long id, KnowledgeFolder update) {
        KnowledgeFolder existing = getById(id);
        existing.setName(update.getName());
        existing.setParentId(update.getParentId());
        existing.setSortOrder(update.getSortOrder());
        folderMapper.update(existing);
        return existing;
    }

    @Transactional
    public void delete(Long id) {
        getById(id);
        List<KnowledgeFolder> children = folderMapper.findByParentId(id);
        if (!children.isEmpty()) throw new BusinessException(400, "目录下存在子目录，无法删除");
        folderMapper.delete(id);
    }
}
