package com.superbox.app.aiTranslate.mapper;

import com.superbox.app.aiTranslate.entity.TranslatorRole;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RoleMapper {

    @Select("SELECT * FROM ait_translator_role WHERE id = #{id}")
    TranslatorRole findById(Long id);

    @Select("SELECT * FROM ait_translator_role WHERE user_id = #{userId} OR is_preset = true ORDER BY sort_order, id")
    List<TranslatorRole> findByUserAndPresets(Long userId);

    @Insert("INSERT INTO ait_translator_role (user_id, name, description, default_prompt_id, model, temperature, max_tokens, is_preset, sort_order) " +
            "VALUES (#{userId}, #{name}, #{description}, #{defaultPromptId}, #{model}, #{temperature}, #{maxTokens}, #{isPreset}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TranslatorRole role);

    @Update("UPDATE ait_translator_role SET name = #{name}, description = #{description}, default_prompt_id = #{defaultPromptId}, model = #{model}, temperature = #{temperature}, max_tokens = #{maxTokens}, sort_order = #{sortOrder}, updated_at = NOW() WHERE id = #{id}")
    int update(TranslatorRole role);

    @Delete("DELETE FROM ait_translator_role WHERE id = #{id}")
    int delete(Long id);
}
