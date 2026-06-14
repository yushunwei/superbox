package com.superbox.app.knowledgeBase.mapper;

import org.apache.ibatis.jdbc.SQL;

public class KnowledgeSqlProvider {

    public String listEntries(Long folderId, String keyword, int offset, int size) {
        return buildQuery(folderId, keyword) +
                " ORDER BY e.updated_at DESC LIMIT #{size} OFFSET #{offset}";
    }

    public String countEntries(Long folderId, String keyword) {
        return new SQL() {{
            SELECT("COUNT(*)");
            FROM("knowledge_entry e");
            if (folderId != null) {
                WHERE("e.folder_id = #{folderId}");
            }
            if (keyword != null && !keyword.isEmpty()) {
                WHERE("(e.title ILIKE '%' || #{keyword} || '%' OR e.content_text ILIKE '%' || #{keyword} || '%')");
            }
        }}.toString();
    }

    private String buildQuery(Long folderId, String keyword) {
        return new SQL() {{
            SELECT("e.*");
            FROM("knowledge_entry e");
            if (folderId != null) {
                WHERE("e.folder_id = #{folderId}");
            }
            if (keyword != null && !keyword.isEmpty()) {
                WHERE("(e.title ILIKE '%' || #{keyword} || '%' OR e.content_text ILIKE '%' || #{keyword} || '%')");
            }
        }}.toString();
    }
}
