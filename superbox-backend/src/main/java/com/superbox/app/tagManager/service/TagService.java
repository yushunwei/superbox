package com.superbox.app.tagManager.service;

import com.superbox.app.tagManager.entity.Tag;
import com.superbox.app.tagManager.mapper.TagCategoryMapper;
import com.superbox.app.tagManager.mapper.TagMapper;
import com.superbox.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;
    private final TagCategoryMapper tagCategoryMapper;

    public List<Tag> list(Long categoryId) {
        if (categoryId != null) return tagMapper.findByCategoryId(categoryId);
        return tagMapper.findAll();
    }

    public Tag getById(Long id) {
        Tag tag = tagMapper.findById(id);
        if (tag == null) throw new BusinessException(404, "标签不存在");
        return tag;
    }

    public Tag create(Tag tag) {
        // IS-032/033: name null/blank check
        if (tag.getName() == null || tag.getName().isBlank()) {
            throw new BusinessException(400, "标签名称不能为空");
        }
        // IS-039: strip HTML tags to prevent XSS
        String cleanedName = tag.getName().replaceAll("<[^>]*>", "").trim();
        if (cleanedName.isBlank()) {
            throw new BusinessException(400, "标签名称不能为空");
        }
        tag.setName(cleanedName);
        // IS-036: name length check
        if (tag.getName().length() > 64) {
            throw new BusinessException(400, "标签名称不能超过64个字符");
        }
        // IS-035: verify category exists if specified
        if (tag.getCategoryId() != null) {
            if (tagCategoryMapper.findById(tag.getCategoryId()) == null) {
                throw new BusinessException(400, "指定的分类不存在");
            }
        }
        if (tag.getColor() == null) tag.setColor("#409EFF");
        if (tag.getSortOrder() == null) tag.setSortOrder(0);
        // IS-034: catch duplicate name
        try {
            tagMapper.insert(tag);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(400, "标签名称已存在");
        }
        return tagMapper.findById(tag.getId());
    }

    public Tag update(Long id, Tag tag) {
        Tag existing = getById(id);
        if (tag.getName() != null && !tag.getName().isBlank()) existing.setName(tag.getName());
        if (tag.getColor() != null) existing.setColor(tag.getColor());
        if (tag.getCategoryId() != null) existing.setCategoryId(tag.getCategoryId());
        if (tag.getSortOrder() != null) existing.setSortOrder(tag.getSortOrder());
        tagMapper.update(existing);
        return tagMapper.findById(id);
    }

    public void delete(Long id) {
        Tag tag = getById(id);
        int taskRefs = tagMapper.countTaskRefs(id);
        int knowledgeRefs = tagMapper.countKnowledgeRefs(id);
        if (taskRefs > 0 || knowledgeRefs > 0) {
            StringBuilder msg = new StringBuilder("标签「").append(tag.getName()).append("」已被使用，不能删除。");
            if (taskRefs > 0) msg.append("（").append(taskRefs).append(" 个任务引用了此标签）");
            if (knowledgeRefs > 0) msg.append("（").append(knowledgeRefs).append(" 个知识条目引用了此标签）");
            throw new BusinessException(400, msg.toString());
        }
        tagMapper.delete(id);
    }
}
