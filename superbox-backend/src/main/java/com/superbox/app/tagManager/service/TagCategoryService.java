package com.superbox.app.tagManager.service;

import com.superbox.app.tagManager.entity.TagCategory;
import com.superbox.app.tagManager.mapper.TagCategoryMapper;
import com.superbox.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagCategoryService {

    private final TagCategoryMapper mapper;

    public List<TagCategory> list() {
        return mapper.findAll();
    }

    public TagCategory getById(Long id) {
        TagCategory c = mapper.findById(id);
        if (c == null) throw new BusinessException(404, "分类不存在");
        return c;
    }

    @Transactional
    public TagCategory create(TagCategory category) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new BusinessException(400, "分类名称不能为空");
        }
        if (category.getColor() == null) category.setColor("#409EFF");
        if (category.getSortOrder() == null) category.setSortOrder(0);
        try {
            mapper.insert(category);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(400, "分类名称已存在");
        }
        return mapper.findById(category.getId());
    }

    @Transactional
    public TagCategory update(Long id, TagCategory category) {
        TagCategory existing = getById(id);
        if (category.getName() != null && !category.getName().isBlank()) existing.setName(category.getName());
        if (category.getColor() != null) existing.setColor(category.getColor());
        if (category.getSortOrder() != null) existing.setSortOrder(category.getSortOrder());
        mapper.update(existing);
        return mapper.findById(id);
    }

    @Transactional
    public void delete(Long id) {
        getById(id);
        mapper.delete(id);
    }
}
