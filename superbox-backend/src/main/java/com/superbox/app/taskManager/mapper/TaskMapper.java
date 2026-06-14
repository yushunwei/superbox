package com.superbox.app.taskManager.mapper;

import com.superbox.app.taskManager.entity.TagRef;
import com.superbox.app.taskManager.entity.Task;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface TaskMapper {

    @Select("SELECT * FROM task WHERE id = #{id}")
    Task findById(Long id);

    @SelectProvider(type = TaskSqlProvider.class, method = "listTasks")
    List<Task> list(@Param("status") String status, @Param("priority") String priority,
                    @Param("keyword") String keyword,
                    @Param("executor") String executor,
                    @Param("tagId") String tagId,
                    @Param("offset") int offset, @Param("size") int size);

    @SelectProvider(type = TaskSqlProvider.class, method = "countTasks")
    Long count(@Param("status") String status, @Param("priority") String priority,
               @Param("keyword") String keyword,
               @Param("executor") String executor,
               @Param("tagId") String tagId);

    @Select("SELECT t.id, t.name, t.color FROM tag t " +
            "INNER JOIN task_tag tt ON t.id = tt.tag_id WHERE tt.task_id = #{taskId}")
    List<TagRef> findTagsByTaskId(Long taskId);

    @Insert("INSERT INTO task (title, description, status, priority, plan_start_date, plan_end_date, " +
            "executor, collaborators, remarks, parent_task_id, sort_order) " +
            "VALUES (#{title}, #{description}, #{status}, #{priority}, #{planStartDate}, #{planEndDate}, " +
            "#{executor}, #{collaborators}, #{remarks}, #{parentTaskId}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Task task);

    @Update("UPDATE task SET title = #{title}, description = #{description}, status = #{status}, " +
            "priority = #{priority}, plan_start_date = #{planStartDate}, plan_end_date = #{planEndDate}, " +
            "executor = #{executor}, collaborators = #{collaborators}, remarks = #{remarks}, " +
            "parent_task_id = #{parentTaskId}, sort_order = #{sortOrder}, updated_at = NOW() WHERE id = #{id}")
    int update(Task task);

    @UpdateProvider(type = TaskSqlProvider.class, method = "updateCellSql")
    int updateCell(@Param("id") Long id, @Param("field") String field, @Param("value") Object value);

    @Delete("DELETE FROM task WHERE id = #{id}")
    int delete(Long id);

    @DeleteProvider(type = TaskSqlProvider.class, method = "deleteRowsSql")
    int deleteRows(@Param("ids") List<Long> ids);

    @Update("UPDATE task SET status = #{status}, completed_at = CASE WHEN #{status} = 'completed' THEN NOW() ELSE NULL END, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE task SET sort_order = #{sortOrder}, updated_at = NOW() WHERE id = #{id}")
    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") int sortOrder);

    @UpdateProvider(type = TaskSqlProvider.class, method = "batchUpdateSortOrderSql")
    int batchUpdateSortOrder(@Param("items") List<Map<String, Object>> items);

    @Insert("INSERT INTO task_tag (task_id, tag_id) VALUES (#{taskId}, #{tagId})")
    int insertTag(@Param("taskId") Long taskId, @Param("tagId") Long tagId);

    @Delete("DELETE FROM task_tag WHERE task_id = #{taskId}")
    int deleteTags(Long taskId);

    @Select("SELECT COUNT(*) FROM task WHERE status IN ('not_started', 'in_progress')")
    Long countPending();

    @Select("SELECT * FROM task ORDER BY sort_order ASC FOR UPDATE")
    List<Task> findAllForUpdate();

    @Select("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM task")
    int nextSortOrder();

    @Delete("DELETE FROM task_tag WHERE task_id = #{taskId} AND tag_id = #{tagId}")
    int deleteTaskTag(@Param("taskId") Long taskId, @Param("tagId") Long tagId);
}
