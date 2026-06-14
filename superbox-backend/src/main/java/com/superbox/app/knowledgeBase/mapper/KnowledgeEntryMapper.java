package com.superbox.app.knowledgeBase.mapper;

import com.superbox.app.knowledgeBase.entity.KnowledgeEntry;
import com.superbox.app.taskManager.entity.TagRef;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeEntryMapper {

    @Select("SELECT * FROM knowledge_entry WHERE id = #{id}")
    KnowledgeEntry findById(Long id);

    @SelectProvider(type = KnowledgeSqlProvider.class, method = "listEntries")
    List<KnowledgeEntry> list(@Param("folderId") Long folderId, @Param("keyword") String keyword,
                              @Param("offset") int offset, @Param("size") int size);

    @SelectProvider(type = KnowledgeSqlProvider.class, method = "countEntries")
    Long count(@Param("folderId") Long folderId, @Param("keyword") String keyword);

    @Select("SELECT t.id, t.name, t.color FROM tag t " +
            "INNER JOIN knowledge_tag kt ON t.id = kt.tag_id WHERE kt.entry_id = #{entryId}")
    List<TagRef> findTagsByEntryId(Long entryId);

    @Insert("INSERT INTO knowledge_entry (folder_id, title, source_type, source_id, file_path, file_type, file_size, content_text, chunk_count) " +
            "VALUES (#{folderId}, #{title}, #{sourceType}, #{sourceId}, #{filePath}, #{fileType}, #{fileSize}, #{contentText}, #{chunkCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeEntry entry);

    @Update("UPDATE knowledge_entry SET title = #{title}, content_text = #{contentText}, " +
            "folder_id = #{folderId}, updated_at = NOW() WHERE id = #{id}")
    int update(KnowledgeEntry entry);

    @Update("UPDATE knowledge_entry SET chunk_count = #{chunkCount}, updated_at = NOW() WHERE id = #{id}")
    int updateChunkCount(@Param("id") Long id, @Param("chunkCount") int chunkCount);

    @Delete("DELETE FROM knowledge_entry WHERE id = #{id}")
    int delete(Long id);

    @Insert("INSERT INTO knowledge_tag (entry_id, tag_id) VALUES (#{entryId}, #{tagId})")
    int insertTag(@Param("entryId") Long entryId, @Param("tagId") Long tagId);

    @Delete("DELETE FROM knowledge_tag WHERE entry_id = #{entryId}")
    int deleteTags(Long entryId);
}
