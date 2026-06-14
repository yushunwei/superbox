package com.superbox.app.taskManager.mapper;

import org.apache.ibatis.jdbc.SQL;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TaskSqlProvider {

    private static final Set<String> VALID_STATUSES = Set.of("not_started", "in_progress", "completed", "cancelled");
    private static final Set<String> VALID_PRIORITIES = Set.of("normal", "important");

    public String listTasks(String status, String priority, String keyword,
                            String executor, String tagId, int offset, int size) {
        return buildQuery(status, priority, keyword, executor, tagId) +
                " ORDER BY t.sort_order ASC, t.created_at DESC LIMIT #{size} OFFSET #{offset}";
    }

    public String countTasks(String status, String priority, String keyword, String executor, String tagId) {
        String inner = buildQuery(status, priority, keyword, executor, tagId);
        return "SELECT COUNT(*) FROM (" + inner + ") _count";
    }

    private String buildQuery(String status, String priority, String keyword, String executor, String tagId) {
        return new SQL() {{
            SELECT("DISTINCT t.*");
            FROM("task t");
            if (tagId != null && !tagId.isEmpty()) {
                INNER_JOIN("task_tag tl_filter ON tl_filter.task_id = t.id");
                WHERE("tl_filter.tag_id IN (" + buildNumericInClause(tagId) + ")");
            }
            if (keyword != null && !keyword.isEmpty()) {
                LEFT_OUTER_JOIN("task_tag tl_kw ON tl_kw.task_id = t.id");
                LEFT_OUTER_JOIN("tag t_kw ON t_kw.id = tl_kw.tag_id");
                WHERE("(t.title ILIKE '%' || #{keyword} || '%'" +
                      " OR t.description ILIKE '%' || #{keyword} || '%'" +
                      " OR t.executor ILIKE '%' || #{keyword} || '%'" +
                      " OR t.collaborators ILIKE '%' || #{keyword} || '%'" +
                      " OR t.remarks ILIKE '%' || #{keyword} || '%'" +
                      " OR t_kw.name ILIKE '%' || #{keyword} || '%')");
            }
            if (status != null && !status.isEmpty()) {
                if (status.contains(",")) {
                    WHERE("t.status IN (" + buildWhitelistInClause(status, VALID_STATUSES) + ")");
                } else {
                    WHERE("t.status = #{status}");
                }
            }
            if (priority != null && !priority.isEmpty()) {
                if (priority.contains(",")) {
                    WHERE("t.priority IN (" + buildWhitelistInClause(priority, VALID_PRIORITIES) + ")");
                } else {
                    WHERE("t.priority = #{priority}");
                }
            }
            if (executor != null && !executor.isEmpty()) {
                WHERE("t.executor ILIKE '%' || #{executor} || '%'");
            }
        }}.toString();
    }

    private String buildNumericInClause(String csv) {
        String[] parts = csv.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String trimmed = parts[i].trim();
            try {
                Long.parseLong(trimmed);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid numeric ID: " + trimmed);
            }
            if (i > 0) sb.append(", ");
            sb.append(trimmed);
        }
        return sb.toString();
    }

    private String buildWhitelistInClause(String csv, Set<String> whitelist) {
        String[] parts = csv.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String trimmed = parts[i].trim();
            if (!whitelist.contains(trimmed)) {
                throw new IllegalArgumentException("Invalid value: " + trimmed);
            }
            if (i > 0) sb.append(", ");
            sb.append("'").append(trimmed).append("'");
        }
        return sb.toString();
    }

    public String updateCellSql(Map<String, Object> params) {
        String field = (String) params.get("field");
        // Whitelist allowed column names to prevent SQL injection
        String column = switch (field) {
            case "title" -> "title";
            case "description" -> "description";
            case "status" -> "status";
            case "priority" -> "priority";
            case "planStartDate" -> "plan_start_date";
            case "planEndDate" -> "plan_end_date";
            case "executor" -> "executor";
            case "collaborators" -> "collaborators";
            case "remarks" -> "remarks";
            case "completedAt" -> "completed_at";
            case "sortOrder" -> "sort_order";
            default -> throw new IllegalArgumentException("Invalid field: " + field);
        };
        return "UPDATE task SET " + column + " = #{value}, updated_at = NOW() WHERE id = #{id}";
    }

    public String deleteRowsSql(Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) params.get("ids");
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ids must not be null or empty");
        }
        StringBuilder sb = new StringBuilder("DELETE FROM task WHERE id IN (");
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            if (id == null || id <= 0) {
                throw new IllegalArgumentException("Invalid task id at index " + i + ": " + id);
            }
            if (i > 0) sb.append(", ");
            sb.append(id);
        }
        sb.append(")");
        return sb.toString();
    }

    public String batchUpdateSortOrderSql(Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) params.get("items");
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE task SET sort_order = CASE id ");
        for (Map<String, Object> item : items) {
            Object idObj = item.get("id");
            Object sortObj = item.get("sortOrder");
            if (!(idObj instanceof Number) || !(sortObj instanceof Number)) {
                throw new IllegalArgumentException("id and sortOrder must be numbers");
            }
            sb.append("WHEN ").append(((Number) idObj).longValue()).append(" THEN ").append(((Number) sortObj).intValue()).append(" ");
        }
        sb.append("END, updated_at = NOW() WHERE id IN (");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(", ");
            Object idObj = items.get(i).get("id");
            if (!(idObj instanceof Number)) {
                throw new IllegalArgumentException("id must be a number");
            }
            sb.append(((Number) idObj).longValue());
        }
        sb.append(")");
        return sb.toString();
    }
}
