package com.superbox.app.aiChat.mapper;

import com.superbox.app.aiChat.entity.Conversation;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ConversationMapper {

    @Select("SELECT * FROM ai_chat_conversation ORDER BY updated_at DESC")
    List<Conversation> findAll();

    @Select("SELECT * FROM ai_chat_conversation WHERE id = #{id}")
    Conversation findById(Long id);

    @Insert("INSERT INTO ai_chat_conversation (title, model, system_prompt, message_count) " +
            "VALUES (#{title}, #{model}, #{systemPrompt}, #{messageCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Conversation conv);

    @Update("UPDATE ai_chat_conversation SET title = #{title}, model = #{model}, message_count = #{messageCount}, updated_at = NOW() WHERE id = #{id}")
    int update(Conversation conv);

    @Delete("DELETE FROM ai_chat_conversation WHERE id = #{id}")
    int delete(Long id);
}
