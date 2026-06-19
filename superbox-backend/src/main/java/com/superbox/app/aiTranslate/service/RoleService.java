package com.superbox.app.aiTranslate.service;

import com.superbox.app.aiTranslate.entity.TranslatorRole;
import com.superbox.app.aiTranslate.mapper.RoleMapper;
import com.superbox.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;

    public List<TranslatorRole> list(Long userId) {
        return roleMapper.findByUserAndPresets(userId);
    }

    public TranslatorRole getById(Long id) {
        TranslatorRole role = roleMapper.findById(id);
        if (role == null) {
            throw new BusinessException(404, "翻译角色不存在");
        }
        return role;
    }

    @Transactional
    public TranslatorRole create(TranslatorRole role) {
        validateRole(role);
        if (role.getIsPreset() == null) role.setIsPreset(false);
        if (role.getSortOrder() == null) role.setSortOrder(0);
        if (role.getTemperature() == null) role.setTemperature(0.3);
        if (role.getMaxTokens() == null) role.setMaxTokens(4096);
        roleMapper.insert(role);
        log.info("Created translator role id={}, name={}", role.getId(), role.getName());
        return roleMapper.findById(role.getId());
    }

    @Transactional
    public TranslatorRole update(Long id, TranslatorRole role) {
        TranslatorRole existing = getById(id);
        if (role.getName() != null) existing.setName(role.getName());
        if (role.getDescription() != null) existing.setDescription(role.getDescription());
        if (role.getDefaultPromptId() != null) existing.setDefaultPromptId(role.getDefaultPromptId());
        if (role.getModel() != null) existing.setModel(role.getModel());
        if (role.getTemperature() != null) existing.setTemperature(role.getTemperature());
        if (role.getMaxTokens() != null) existing.setMaxTokens(role.getMaxTokens());
        if (role.getSortOrder() != null) existing.setSortOrder(role.getSortOrder());
        validateRole(existing);
        roleMapper.update(existing);
        log.info("Updated translator role id={}", id);
        return roleMapper.findById(id);
    }

    @Transactional
    public void delete(Long id) {
        TranslatorRole role = getById(id);
        if (Boolean.TRUE.equals(role.getIsPreset())) {
            throw new BusinessException(400, "系统预设角色不能删除");
        }
        roleMapper.delete(id);
        log.info("Deleted translator role id={}", id);
    }

    private void validateRole(TranslatorRole role) {
        if (role.getName() == null || role.getName().isBlank()) {
            throw new BusinessException(400, "角色名称不能为空");
        }
        if (role.getName().length() > 100) {
            throw new BusinessException(400, "角色名称不能超过100个字符");
        }
    }
}
