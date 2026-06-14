package com.superbox.app.knowledgeBase.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.SelectKey;

import java.util.Map;

@Mapper
public interface KnowledgeMapper {

    @Insert("INSERT INTO knowledge_entry (folder_id, title, source_type, source_id, content_text) " +
            "VALUES (#{folderId}, #{title}, 'imported_task', #{sourceId}, #{content})")
    @SelectKey(statement = "SELECT currval('knowledge_entry_id_seq')", keyProperty = "id", before = false, resultType = Long.class)
    long insertFromTask(Map<String, Object> params);
}
