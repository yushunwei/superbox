package com.superbox.app.knowledgeBase.mapper;

import com.superbox.app.knowledgeBase.entity.KnowledgeFolder;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeFolderMapper {

    @Select("SELECT * FROM knowledge_folder ORDER BY sort_order, id")
    List<KnowledgeFolder> findAll();

    @Select("SELECT * FROM knowledge_folder WHERE id = #{id}")
    KnowledgeFolder findById(Long id);

    @Select("SELECT * FROM knowledge_folder WHERE parent_id = #{parentId} ORDER BY sort_order, id")
    List<KnowledgeFolder> findByParentId(Long parentId);

    @Insert("INSERT INTO knowledge_folder (name, parent_id, sort_order) VALUES (#{name}, #{parentId}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeFolder folder);

    @Update("UPDATE knowledge_folder SET name = #{name}, parent_id = #{parentId}, sort_order = #{sortOrder}, updated_at = NOW() WHERE id = #{id}")
    int update(KnowledgeFolder folder);

    @Delete("DELETE FROM knowledge_folder WHERE id = #{id}")
    int delete(Long id);

    @Update("UPDATE knowledge_folder SET entry_count = entry_count + #{delta} WHERE id = #{id}")
    int updateEntryCount(@Param("id") Long id, @Param("delta") int delta);

    @Update("UPDATE knowledge_folder SET sort_order = #{sortOrder}, updated_at = NOW() WHERE id = #{id}")
    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") int sortOrder);
}
