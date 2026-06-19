package com.superbox.app.aiTranslate.service;

import com.superbox.app.aiTranslate.entity.PromptTemplate;
import com.superbox.app.aiTranslate.mapper.PromptMapper;
import com.superbox.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

    private final PromptMapper promptMapper;

    public List<PromptTemplate> list(Long userId, String category) {
        return promptMapper.findByUserAndPresets(userId, category);
    }

    public PromptTemplate getById(Long id) {
        PromptTemplate template = promptMapper.findById(id);
        if (template == null) {
            throw new BusinessException(404, "提示词模板不存在");
        }
        return template;
    }

    @Transactional
    public PromptTemplate create(PromptTemplate template) {
        validateTemplate(template);
        if (template.getIsPreset() == null) template.setIsPreset(false);
        if (template.getSortOrder() == null) template.setSortOrder(0);
        promptMapper.insert(template);
        log.info("Created prompt template id={}, name={}", template.getId(), template.getName());
        return promptMapper.findById(template.getId());
    }

    @Transactional
    public PromptTemplate update(Long id, PromptTemplate template) {
        PromptTemplate existing = getById(id);
        if (template.getName() != null) existing.setName(template.getName());
        if (template.getCategory() != null) existing.setCategory(template.getCategory());
        if (template.getSystemPrompt() != null) existing.setSystemPrompt(template.getSystemPrompt());
        if (template.getSortOrder() != null) existing.setSortOrder(template.getSortOrder());
        validateTemplate(existing);
        promptMapper.update(existing);
        log.info("Updated prompt template id={}", id);
        return promptMapper.findById(id);
    }

    @Transactional
    public void delete(Long id) {
        PromptTemplate template = getById(id);
        if (Boolean.TRUE.equals(template.getIsPreset())) {
            throw new BusinessException(400, "系统预设模板不能删除");
        }
        promptMapper.delete(id);
        log.info("Deleted prompt template id={}", id);
    }

    private void validateTemplate(PromptTemplate template) {
        if (template.getName() == null || template.getName().isBlank()) {
            throw new BusinessException(400, "模板名称不能为空");
        }
        if (template.getName().length() > 100) {
            throw new BusinessException(400, "模板名称不能超过100个字符");
        }
        if (template.getSystemPrompt() == null || template.getSystemPrompt().isBlank()) {
            throw new BusinessException(400, "系统提示词不能为空");
        }
    }
}
