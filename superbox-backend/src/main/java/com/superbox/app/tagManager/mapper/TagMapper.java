package com.superbox.app.tagManager.mapper;

import com.superbox.app.tagManager.entity.Tag;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TagMapper {

    @Select("SELECT * FROM tag ORDER BY sort_order, id")
    List<Tag> findAll();

    @Select("SELECT * FROM tag WHERE category_id = #{categoryId} ORDER BY sort_order, id")
    List<Tag> findByCategoryId(Long categoryId);

    @Select("SELECT * FROM tag WHERE id = #{id}")
    Tag findById(Long id);

    @Insert("INSERT INTO tag (name, color, category_id, sort_order) VALUES (#{name}, #{color}, #{categoryId}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Tag tag);

    @Update("UPDATE tag SET name = #{name}, color = #{color}, category_id = #{categoryId}, sort_order = #{sortOrder} WHERE id = #{id}")
    int update(Tag label);

    @Delete("DELETE FROM tag WHERE id = #{id}")
    int delete(Long id);

    @Select("SELECT COUNT(*) FROM task_tag WHERE tag_id = #{tagId}")
    int countTaskRefs(Long tagId);

    @Select("SELECT COUNT(*) FROM knowledge_tag WHERE tag_id = #{tagId}")
    int countKnowledgeRefs(Long tagId);
}
