package com.superbox.app.aiTranslate.mapper;

import com.superbox.app.aiTranslate.entity.UserTranslateSettings;
import org.apache.ibatis.annotations.*;

@Mapper
public interface TranslateSettingsMapper {

    @Select("SELECT * FROM ait_translate_settings WHERE user_id = #{userId}")
    UserTranslateSettings findByUserId(Long userId);

    @Insert("INSERT INTO ait_translate_settings (user_id, default_model, default_source_lang, default_target_lang, temperature, max_tokens, pdf_format, word_format, doc2x_enabled) " +
            "VALUES (#{userId}, #{defaultModel}, #{defaultSourceLang}, #{defaultTargetLang}, #{temperature}, #{maxTokens}, #{pdfFormat}, #{wordFormat}, #{doc2xEnabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserTranslateSettings settings);

    @Update("UPDATE ait_translate_settings SET default_model = #{defaultModel}, default_source_lang = #{defaultSourceLang}, " +
            "default_target_lang = #{defaultTargetLang}, temperature = #{temperature}, max_tokens = #{maxTokens}, " +
            "pdf_format = #{pdfFormat}, word_format = #{wordFormat}, doc2x_enabled = #{doc2xEnabled}, updated_at = NOW() " +
            "WHERE user_id = #{userId}")
    int update(UserTranslateSettings settings);
}
