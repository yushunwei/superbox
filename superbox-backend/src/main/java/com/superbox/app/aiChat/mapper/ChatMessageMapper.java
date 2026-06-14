package com.superbox.app.aiChat.mapper;

import com.superbox.app.aiChat.entity.ChatMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    @Select("SELECT * FROM ai_chat_message WHERE conversation_id = #{conversationId} ORDER BY created_at ASC")
    List<ChatMessage> findByConversationId(Long conversationId);

    @Select("SELECT * FROM ai_chat_message WHERE conversation_id = #{conversationId} ORDER BY created_at DESC LIMIT #{limit}")
    List<ChatMessage> findRecent(@Param("conversationId") Long conversationId, @Param("limit") int limit);

    @Insert("INSERT INTO ai_chat_message (conversation_id, role, content, reasoning_content, sources, token_count, model) " +
            "VALUES (#{conversationId}, #{role}, #{content}, #{reasoningContent}, #{sources}, #{tokenCount}, #{model})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessage msg);

    @Delete("DELETE FROM ai_chat_message WHERE conversation_id = #{conversationId}")
    int deleteByConversationId(Long conversationId);
}
