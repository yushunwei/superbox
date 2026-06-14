package com.superbox.app.tagManager.mapper;

import com.superbox.app.tagManager.entity.TagCategory;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TagCategoryMapper {

    @Select("SELECT * FROM tag_category ORDER BY sort_order, id")
    List<TagCategory> findAll();

    @Select("SELECT * FROM tag_category WHERE id = #{id}")
    TagCategory findById(Long id);

    @Insert("INSERT INTO tag_category (name, color, sort_order) VALUES (#{name}, #{color}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TagCategory category);

    @Update("UPDATE tag_category SET name = #{name}, color = #{color}, sort_order = #{sortOrder} WHERE id = #{id}")
    int update(TagCategory category);

    @Delete("DELETE FROM tag_category WHERE id = #{id}")
    int delete(Long id);
}
