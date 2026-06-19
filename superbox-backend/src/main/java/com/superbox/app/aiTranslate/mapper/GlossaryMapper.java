package com.superbox.app.aiTranslate.mapper;

import com.superbox.app.aiTranslate.entity.GlossaryEntry;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface GlossaryMapper {

    @Select("SELECT * FROM ait_glossary_entry WHERE id = #{id}")
    GlossaryEntry findById(Long id);

    @Select("SELECT * FROM ait_glossary_entry WHERE user_id = #{userId} AND source_lang = #{sourceLang} AND target_lang = #{targetLang} AND (source_term ILIKE '%' || #{keyword} || '%' OR target_term ILIKE '%' || #{keyword} || '%') ORDER BY id LIMIT #{size} OFFSET #{offset}")
    List<GlossaryEntry> findByUserAndLangs(Long userId, String sourceLang, String targetLang, String keyword, int offset, int size);

    @Select("SELECT COUNT(*) FROM ait_glossary_entry WHERE user_id = #{userId} AND source_lang = #{sourceLang} AND target_lang = #{targetLang} AND (source_term ILIKE '%' || #{keyword} || '%' OR target_term ILIKE '%' || #{keyword} || '%')")
    long countByUserAndLangs(Long userId, String sourceLang, String targetLang, String keyword);

    @Insert("INSERT INTO ait_glossary_entry (user_id, source_lang, target_lang, source_term, target_term, category, note) " +
            "VALUES (#{userId}, #{sourceLang}, #{targetLang}, #{sourceTerm}, #{targetTerm}, #{category}, #{note})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(GlossaryEntry entry);

    @Insert("<script>" +
            "INSERT INTO ait_glossary_entry (user_id, source_lang, target_lang, source_term, target_term, category, note) VALUES " +
            "<foreach collection='list' item='entry' separator=','>" +
            "(#{entry.userId}, #{entry.sourceLang}, #{entry.targetLang}, #{entry.sourceTerm}, #{entry.targetTerm}, #{entry.category}, #{entry.note})" +
            "</foreach>" +
            "</script>")
    int batchInsert(List<GlossaryEntry> entries);

    @Update("UPDATE ait_glossary_entry SET source_lang = #{sourceLang}, target_lang = #{targetLang}, source_term = #{sourceTerm}, target_term = #{targetTerm}, category = #{category}, note = #{note}, updated_at = NOW() WHERE id = #{id}")
    int update(GlossaryEntry entry);

    @Delete("DELETE FROM ait_glossary_entry WHERE id = #{id}")
    int delete(Long id);
}
