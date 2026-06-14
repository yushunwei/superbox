package com.superbox.app.taskManager.service;

import com.superbox.app.knowledgeBase.mapper.KnowledgeMapper;
import com.superbox.app.taskManager.entity.Task;
import com.superbox.app.taskManager.mapper.TaskMapper;
import com.superbox.common.BusinessException;
import com.superbox.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final KnowledgeMapper knowledgeMapper;

    private static final Set<String> VALID_STATUSES = Set.of("not_started", "in_progress", "completed", "cancelled");
    private static final Set<String> VALID_PRIORITIES = Set.of("normal", "important");

    public PageResult<Task> list(String status, String priority, String keyword, String executor, String tagId, int page, int size) {
        int offset = (page - 1) * size;
        List<Task> tasks = taskMapper.list(status, priority, keyword, executor, tagId, offset, size);
        for (Task task : tasks) {
            task.setTags(taskMapper.findTagsByTaskId(task.getId()));
        }
        long total = taskMapper.count(status, priority, keyword, executor, tagId);
        return new PageResult<>(tasks, total, page, size);
    }

    public Task getById(Long id) {
        Task task = taskMapper.findById(id);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        task.setTags(taskMapper.findTagsByTaskId(id));
        return task;
    }

    @Transactional
    public Task create(Task task, List<Long> tagIds) {
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            throw new BusinessException(400, "标题不能为空");
        }
        if (task.getTitle().length() > 256) {
            throw new BusinessException(400, "标题长度不能超过256个字符");
        }
        if (task.getExecutor() != null && task.getExecutor().length() > 128) {
            throw new BusinessException(400, "执行人长度不能超过128个字符");
        }
        if (task.getCollaborators() != null && task.getCollaborators().length() > 512) {
            throw new BusinessException(400, "协同人长度不能超过512个字符");
        }
        if (task.getStatus() == null) task.setStatus("not_started");
        if (task.getPriority() == null) task.setPriority("normal");
        if (task.getSortOrder() == null) {
            task.setSortOrder(taskMapper.nextSortOrder());
        }
        validatePlanDates(task.getPlanStartDate(), task.getPlanEndDate());
        taskMapper.insert(task);
        if (tagIds != null) {
            for (Long tagId : tagIds) {
                taskMapper.insertTag(task.getId(), tagId);
            }
        }
        task.setTags(taskMapper.findTagsByTaskId(task.getId()));
        return task;
    }

    @Transactional
    public Task update(Long id, Task update, List<Long> tagIds) {
        if (update.getTitle() != null && update.getTitle().isBlank()) {
            throw new BusinessException(400, "标题不能为空");
        }
        if (update.getTitle() != null && update.getTitle().length() > 256) {
            throw new BusinessException(400, "标题长度不能超过256个字符");
        }
        if (update.getExecutor() != null && update.getExecutor().length() > 128) {
            throw new BusinessException(400, "执行人长度不能超过128个字符");
        }
        if (update.getCollaborators() != null && update.getCollaborators().length() > 512) {
            throw new BusinessException(400, "协同人长度不能超过512个字符");
        }
        Task existing = getById(id);
        if (update.getTitle() != null) existing.setTitle(update.getTitle());
        if (update.getDescription() != null) existing.setDescription(update.getDescription());
        if (update.getPriority() != null) existing.setPriority(update.getPriority());
        if (update.getStatus() != null) existing.setStatus(update.getStatus());
        if (update.getPlanStartDate() != null) existing.setPlanStartDate(update.getPlanStartDate());
        if (update.getPlanEndDate() != null) existing.setPlanEndDate(update.getPlanEndDate());
        if (update.getExecutor() != null) existing.setExecutor(update.getExecutor());
        if (update.getCollaborators() != null) existing.setCollaborators(update.getCollaborators());
        if (update.getRemarks() != null) existing.setRemarks(update.getRemarks());
        if (update.getParentTaskId() != null) existing.setParentTaskId(update.getParentTaskId());
        if (update.getSortOrder() != null) existing.setSortOrder(update.getSortOrder());
        validatePlanDates(existing.getPlanStartDate(), existing.getPlanEndDate());
        taskMapper.update(existing);
        if (tagIds != null) {
            taskMapper.deleteTags(id);
            for (Long tagId : tagIds) {
                taskMapper.insertTag(id, tagId);
            }
        }
        return getById(id);
    }

    @Transactional
    public Task updateCell(Long id, String field, Object value) {
        Task existing = getById(id);
        Object dbValue = value;
        if (value instanceof String str && (field.equals("planStartDate") || field.equals("planEndDate"))) {
            dbValue = !str.isEmpty() ? LocalDate.parse(str) : null;
        } else if (field.equals("completedAt") && value instanceof String str) {
            if (str.isEmpty()) {
                dbValue = null;
            } else if (str.contains("T")) {
                dbValue = LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else {
                dbValue = LocalDate.parse(str).atStartOfDay();
            }
        }
        if (field.equals("title")) {
            if (value == null || value.toString().isBlank()) {
                throw new BusinessException(400, "标题不能为空");
            }
            if (value.toString().length() > 256) {
                throw new BusinessException(400, "标题长度不能超过256个字符");
            }
            taskMapper.updateCell(id, field, dbValue);
        } else if (field.equals("planStartDate") || field.equals("planEndDate")) {
            LocalDate planStartDate = field.equals("planStartDate") ? (LocalDate) dbValue : existing.getPlanStartDate();
            LocalDate planEndDate = field.equals("planEndDate") ? (LocalDate) dbValue : existing.getPlanEndDate();
            validatePlanDates(planStartDate, planEndDate);
            taskMapper.updateCell(id, field, dbValue);
        } else if (field.equals("status")) {
            String newStatus = (String) value;
            if (!VALID_STATUSES.contains(newStatus)) {
                throw new BusinessException(400, "无效的状态值: " + newStatus);
            }
            taskMapper.updateStatus(id, newStatus);
        } else if (field.equals("priority")) {
            String newPriority = (String) value;
            if (!VALID_PRIORITIES.contains(newPriority)) {
                throw new BusinessException(400, "无效的优先级值: " + newPriority);
            }
            taskMapper.updateCell(id, field, dbValue);
        } else {
            taskMapper.updateCell(id, field, dbValue);
        }
        return getById(id);
    }

    @Transactional
    public void delete(Long id) {
        getById(id);
        taskMapper.delete(id);
    }

    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        taskMapper.deleteRows(ids);
    }

    @Transactional
    public List<Task> batchPaste(Long afterId, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return List.of();

        int sortOrder;
        if (afterId != null && afterId > 0) {
            Task afterTask = getById(afterId);
            sortOrder = afterTask.getSortOrder() + 1;
        } else {
            sortOrder = taskMapper.nextSortOrder();
        }

        List<Task> allTasks = taskMapper.findAllForUpdate();
        for (Task t : allTasks) {
            if (t.getSortOrder() >= sortOrder) {
                taskMapper.updateSortOrder(t.getId(), t.getSortOrder() + rows.size());
            }
        }

        List<Task> created = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            Task task = new Task();
            task.setTitle(getString(row, "title", "[粘贴任务]"));
            task.setDescription(getString(row, "description", null));
            task.setStatus(validStatus(getString(row, "status", "not_started")));
            task.setPriority(validPriority(getString(row, "priority", "normal")));
            task.setExecutor(getString(row, "executor", null));
            task.setCollaborators(getString(row, "collaborators", null));
            task.setRemarks(getString(row, "remarks", null));

            String psd = getString(row, "planStartDate", null);
            if (psd != null && !psd.isEmpty()) task.setPlanStartDate(LocalDate.parse(psd));
            String ped = getString(row, "planEndDate", null);
            if (ped != null && !ped.isEmpty()) task.setPlanEndDate(LocalDate.parse(ped));

            task.setSortOrder(sortOrder + i);
            taskMapper.insert(task);

            String status = getString(row, "status", "not_started");
            if ("completed".equals(status)) {
                taskMapper.updateStatus(task.getId(), "completed");
            }

            created.add(getById(task.getId()));
        }
        return created;
    }

    private void validatePlanDates(LocalDate planStartDate, LocalDate planEndDate) {
        if (planStartDate != null && planEndDate != null && planEndDate.isBefore(planStartDate)) {
            throw new BusinessException(400, "计划结束日期不能早于计划开始日期");
        }
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        if (val == null) return defaultValue;
        return val.toString();
    }

    private String validStatus(String s) {
        return VALID_STATUSES.contains(s) ? s : "not_started";
    }

    private String validPriority(String s) {
        return VALID_PRIORITIES.contains(s) ? s : "normal";
    }

    @Transactional
    public Task insertRow(Long afterId) {
        int sortOrder;
        if (afterId != null && afterId > 0) {
            Task afterTask = getById(afterId);
            sortOrder = afterTask.getSortOrder() + 1;
            List<Task> allTasks = taskMapper.findAllForUpdate();
            for (Task t : allTasks) {
                if (t.getSortOrder() >= sortOrder) {
                    taskMapper.updateSortOrder(t.getId(), t.getSortOrder() + 1);
                }
            }
        } else {
            sortOrder = taskMapper.nextSortOrder();
        }

        Task task = new Task();
        task.setTitle("[新任务]");
        task.setStatus("not_started");
        task.setPriority("normal");
        task.setSortOrder(sortOrder);
        taskMapper.insert(task);
        return getById(task.getId());
    }

    @Transactional
    public void updateSortOrders(List<Map<String, Object>> items) {
        taskMapper.batchUpdateSortOrder(items);
    }

    @Transactional
    public Task updateStatus(Long id, String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new BusinessException(400, "无效的状态值: " + status);
        }
        getById(id);
        taskMapper.updateStatus(id, status);
        return getById(id);
    }

    public long countPending() {
        return taskMapper.countPending();
    }

    @Transactional
    public Long promoteToKnowledge(Long id, Long folderId) {
        Task task = getById(id);
        Map<String, Object> params = new HashMap<>();
        params.put("folderId", folderId);
        params.put("title", task.getTitle());
        params.put("sourceId", id);
        params.put("content", task.getDescription() != null ? task.getDescription() : "");
        return knowledgeMapper.insertFromTask(params);
    }

    @Transactional
    public Task setTaskTags(Long taskId, List<Long> tagIds) {
        getById(taskId);
        taskMapper.deleteTags(taskId);
        if (tagIds != null) {
            for (Long tagId : tagIds) {
                taskMapper.insertTag(taskId, tagId);
            }
        }
        return getById(taskId);
    }

    @Transactional
    public void removeTaskTag(Long taskId, Long tagId) {
        getById(taskId);
        taskMapper.deleteTaskTag(taskId, tagId);
    }
}
