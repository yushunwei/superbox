package com.superbox.app.taskManager.controller;

import com.superbox.app.taskManager.entity.Task;
import com.superbox.app.taskManager.service.TaskService;
import com.superbox.common.Result;
import com.superbox.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.superbox.app.taskManager.service.TaskExcelService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/task-manager")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskExcelService taskExcelService;

    @GetMapping
    public Result<PageResult<Task>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String executor,
            @RequestParam(required = false) String tagId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return Result.ok(taskService.list(status, priority, keyword, executor, tagId, page, size));
    }

    @GetMapping("/{id}")
    public Result<Task> getById(@PathVariable Long id) {
        return Result.ok(taskService.getById(id));
    }

    @PostMapping
    public Result<Task> create(@RequestBody Map<String, Object> body) {
        Task task = new Task();
        task.setTitle((String) body.get("title"));
        task.setDescription((String) body.get("description"));
        task.setPriority((String) body.getOrDefault("priority", "normal"));
        task.setStatus((String) body.getOrDefault("status", "not_started"));
        if (body.get("planStartDate") != null) {
            String dateStr = ((String) body.get("planStartDate")).trim();
            if (!dateStr.isEmpty()) {
                task.setPlanStartDate(LocalDate.parse(dateStr));
            }
        }
        if (body.get("planEndDate") != null) {
            String dateStr = ((String) body.get("planEndDate")).trim();
            if (!dateStr.isEmpty()) {
                task.setPlanEndDate(LocalDate.parse(dateStr));
            }
        }
        task.setExecutor((String) body.get("executor"));
        task.setCollaborators((String) body.get("collaborators"));
        task.setRemarks((String) body.get("remarks"));
        @SuppressWarnings("unchecked")
        List<?> rawTagIds = (List<?>) body.get("tagIds");
        List<Long> tagIds = rawTagIds != null ? rawTagIds.stream().map(n -> ((Number) n).longValue()).toList() : null;
        return Result.ok(taskService.create(task, tagIds));
    }

    @PutMapping("/{id}")
    public Result<Task> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Task task = new Task();
        task.setTitle((String) body.get("title"));
        task.setDescription((String) body.get("description"));
        task.setPriority((String) body.get("priority"));
        task.setStatus((String) body.get("status"));
        if (body.get("planStartDate") != null) {
            String dateStr = ((String) body.get("planStartDate")).trim();
            if (!dateStr.isEmpty()) task.setPlanStartDate(LocalDate.parse(dateStr));
        }
        if (body.get("planEndDate") != null) {
            String dateStr = ((String) body.get("planEndDate")).trim();
            if (!dateStr.isEmpty()) task.setPlanEndDate(LocalDate.parse(dateStr));
        }
        task.setExecutor((String) body.get("executor"));
        task.setCollaborators((String) body.get("collaborators"));
        task.setRemarks((String) body.get("remarks"));
        @SuppressWarnings("unchecked")
        List<?> rawTagIds = (List<?>) body.get("tagIds");
        List<Long> tagIds = rawTagIds != null ? rawTagIds.stream().map(n -> ((Number) n).longValue()).toList() : null;
        return Result.ok(taskService.update(id, task, tagIds));
    }

    @PatchMapping("/{id}/cell")
    public Result<Task> updateCell(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String field = (String) body.get("field");
        Object value = body.get("value");
        return Result.ok(taskService.updateCell(id, field, value));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return Result.ok();
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<?> rawIds = (List<?>) body.get("ids");
        List<Long> ids = rawIds.stream().map(n -> ((Number) n).longValue()).toList();
        taskService.batchDelete(ids);
        return Result.ok();
    }

    @PostMapping("/insert-row")
    public Result<Task> insertRow(@RequestBody Map<String, Object> body) {
        Long afterId = body.get("afterId") != null ? ((Number) body.get("afterId")).longValue() : null;
        return Result.ok(taskService.insertRow(afterId));
    }

    @PutMapping("/sort-orders")
    public Result<Void> updateSortOrders(@RequestBody List<Map<String, Object>> items) {
        taskService.updateSortOrders(items);
        return Result.ok();
    }

    @PatchMapping("/{id}/status")
    public Result<Task> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(taskService.updateStatus(id, body.get("status")));
    }

    @PutMapping("/{id}/tags")
    public Result<Task> setTags(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<?> rawIds = (List<?>) body.get("tagIds");
        List<Long> tagIds = rawIds != null ? rawIds.stream().map(n -> ((Number) n).longValue()).toList() : List.of();
        return Result.ok(taskService.setTaskTags(id, tagIds));
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    public Result<Void> removeTag(@PathVariable Long id, @PathVariable Long tagId) {
        taskService.removeTaskTag(id, tagId);
        return Result.ok();
    }

    @PostMapping("/batch-paste")
    public Result<List<Task>> batchPaste(@RequestBody Map<String, Object> body) {
        Long afterId = body.get("afterId") != null ? ((Number) body.get("afterId")).longValue() : null;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("rows");
        return Result.ok(taskService.batchPaste(afterId, rows));
    }

    @PostMapping("/{id}/promote-to-knowledge")
    public Result<Map<String, Object>> promoteToKnowledge(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        Long folderId = body != null && body.get("folderId") != null
                ? ((Number) body.get("folderId")).longValue() : null;
        Long knowledgeId = taskService.promoteToKnowledge(id, folderId);
        return Result.ok(Map.of("knowledgeId", knowledgeId));
    }

    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) {
        taskExcelService.downloadTemplate(response);
    }

    @GetMapping("/export")
    public void exportTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String executor,
            @RequestParam(required = false) String tagId,
            HttpServletResponse response) {
        taskExcelService.exportTasks(status, priority, keyword, executor, tagId, response);
    }

    @PostMapping("/import")
    public Result<Map<String, Object>> importTasks(@RequestParam("file") MultipartFile file) {
        return Result.ok(taskExcelService.importTasks(file));
    }
}
