package com.superbox.app.aiTranslate.mapper;

import com.superbox.app.aiTranslate.entity.TranslateTask;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TranslateTaskMapper {

    @Select("SELECT * FROM ait_translate_task WHERE id = #{id}")
    TranslateTask findById(Long id);

    @Select("SELECT * FROM ait_translate_task WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{size} OFFSET #{offset}")
    List<TranslateTask> findByUserId(Long userId, int offset, int size);

    @Select("SELECT COUNT(*) FROM ait_translate_task WHERE user_id = #{userId}")
    long countByUserId(Long userId);

    @Insert("INSERT INTO ait_translate_task (user_id, file_name, file_size, file_path, output_path, file_type, source_lang, target_lang, model, prompt_template_id, role_id, status, progress, total_segments, completed_segments, error_msg) " +
            "VALUES (#{userId}, #{fileName}, #{fileSize}, #{filePath}, #{outputPath}, #{fileType}, #{sourceLang}, #{targetLang}, #{model}, #{promptTemplateId}, #{roleId}, #{status}, #{progress}, #{totalSegments}, #{completedSegments}, #{errorMsg})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TranslateTask task);

    @Update("UPDATE ait_translate_task SET status = #{status}, error_msg = #{errorMsg}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(Long id, String status, String errorMsg);

    @Update("UPDATE ait_translate_task SET progress = #{progress}, completed_segments = #{completedSegments}, updated_at = NOW() WHERE id = #{id}")
    int updateProgress(Long id, Integer progress, Integer completedSegments);

    @Update("UPDATE ait_translate_task SET output_path = #{outputPath}, status = #{status}, progress = #{progress}, updated_at = NOW() WHERE id = #{id}")
    int updateOutput(Long id, String outputPath, String status, Integer progress);

    @Update("UPDATE ait_translate_task SET status = 'queued', progress = 0, completed_segments = 0, error_msg = NULL, updated_at = NOW() WHERE id = #{id}")
    int resetTask(Long id);

    @Delete("DELETE FROM ait_translate_task WHERE id = #{id}")
    int delete(Long id);
}
