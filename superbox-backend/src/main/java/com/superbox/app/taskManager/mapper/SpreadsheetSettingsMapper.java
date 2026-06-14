package com.superbox.app.taskManager.mapper;

import com.superbox.app.taskManager.entity.SpreadsheetSettings;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SpreadsheetSettingsMapper {

    @Select("SELECT id, user_id, page_key, " +
            "col_widths::text AS col_widths, col_aligns::text AS col_aligns, " +
            "col_wraps::text AS col_wraps, row_heights::text AS row_heights, " +
            "updated_at FROM spreadsheet_settings WHERE user_id = #{userId} AND page_key = #{pageKey}")
    SpreadsheetSettings findByUserAndPage(@Param("userId") Long userId, @Param("pageKey") String pageKey);

    @Insert("INSERT INTO spreadsheet_settings (user_id, page_key, col_widths, col_aligns, col_wraps, row_heights) " +
            "VALUES (#{userId}, #{pageKey}, #{colWidths}::jsonb, #{colAligns}::jsonb, #{colWraps}::jsonb, #{rowHeights}::jsonb) " +
            "ON CONFLICT (user_id, page_key) DO UPDATE SET " +
            "col_widths = EXCLUDED.col_widths, col_aligns = EXCLUDED.col_aligns, " +
            "col_wraps = EXCLUDED.col_wraps, row_heights = EXCLUDED.row_heights, updated_at = NOW()")
    int upsert(SpreadsheetSettings settings);
}
