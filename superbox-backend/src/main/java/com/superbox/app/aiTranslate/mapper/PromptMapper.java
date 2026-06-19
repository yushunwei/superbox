package com.superbox.app.aiTranslate.mapper;

import com.superbox.app.aiTranslate.entity.PromptTemplate;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PromptMapper {

    @Select("SELECT * FROM ait_prompt_template WHERE id = #{id}")
    PromptTemplate findById(Long id);

    @Select("<script>" +
            "SELECT * FROM ait_prompt_template WHERE user_id = #{userId} OR is_preset = true" +
            "<if test='category != null'> AND category = #{category}</if>" +
            " ORDER BY sort_order, id" +
            "</script>")
    List<PromptTemplate> findByUserAndPresets(Long userId, String category);

    @Select("SELECT * FROM ait_prompt_template WHERE category = #{category} ORDER BY sort_order, id")
    List<PromptTemplate> findByCategory(String category);

    @Insert("INSERT INTO ait_prompt_template (user_id, name, category, system_prompt, is_preset, sort_order) " +
            "VALUES (#{userId}, #{name}, #{category}, #{systemPrompt}, #{isPreset}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PromptTemplate template);

    @Update("UPDATE ait_prompt_template SET name = #{name}, category = #{category}, system_prompt = #{systemPrompt}, sort_order = #{sortOrder}, updated_at = NOW() WHERE id = #{id}")
    int update(PromptTemplate template);

    @Delete("DELETE FROM ait_prompt_template WHERE id = #{id}")
    int delete(Long id);
}
