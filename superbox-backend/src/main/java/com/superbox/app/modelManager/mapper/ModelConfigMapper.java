package com.superbox.app.modelManager.mapper;

import com.superbox.app.modelManager.entity.ModelConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ModelConfigMapper {

    @Select("SELECT * FROM aim_model_config WHERE user_id = #{userId} ORDER BY is_default DESC, created_at DESC")
    List<ModelConfig> findByUserId(Long userId);

    @Select("SELECT * FROM aim_model_config WHERE id = #{id}")
    ModelConfig findById(Long id);

    @Select("SELECT * FROM aim_model_config WHERE user_id = #{userId} AND is_default = true LIMIT 1")
    ModelConfig findDefaultByUserId(Long userId);

    @Insert("INSERT INTO aim_model_config (user_id, provider_name, api_key, base_url, model_name, display_name, api_format, is_default, is_active) " +
            "VALUES (#{userId}, #{providerName}, #{apiKey}, #{baseUrl}, #{modelName}, #{displayName}, #{apiFormat}, #{isDefault}, #{isActive})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ModelConfig config);

    @Update("UPDATE aim_model_config SET provider_name = #{providerName}, api_key = #{apiKey}, " +
            "base_url = #{baseUrl}, model_name = #{modelName}, display_name = #{displayName}, " +
            "api_format = #{apiFormat}, is_default = #{isDefault}, is_active = #{isActive}, updated_at = NOW() WHERE id = #{id}")
    int update(ModelConfig config);

    @Delete("DELETE FROM aim_model_config WHERE id = #{id}")
    int delete(Long id);

    @Update("UPDATE aim_model_config SET is_default = false, updated_at = NOW() WHERE user_id = #{userId}")
    int clearDefaults(Long userId);

    @Update("UPDATE aim_model_config SET is_default = true, updated_at = NOW() WHERE id = #{id}")
    int setDefault(Long id);
}
