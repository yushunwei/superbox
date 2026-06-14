package com.superbox.app.knowledgeBase.mapper;

import com.superbox.app.knowledgeBase.entity.KnowledgeChunk;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeChunkMapper {

    @Select("SELECT * FROM knowledge_chunk WHERE entry_id = #{entryId} ORDER BY chunk_index")
    List<KnowledgeChunk> findByEntryId(Long entryId);

    @Insert("INSERT INTO knowledge_chunk (entry_id, chunk_index, content, token_count) " +
            "VALUES (#{entryId}, #{chunkIndex}, #{content}, #{tokenCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeChunk chunk);

    @Delete("DELETE FROM knowledge_chunk WHERE entry_id = #{entryId}")
    int deleteByEntryId(Long entryId);

    @Select("SELECT * FROM knowledge_chunk WHERE entry_id = #{entryId} ORDER BY embedding <=> #{queryEmbedding}::vector LIMIT #{topK}")
    List<KnowledgeChunk> findSimilar(@Param("entryId") Long entryId,
                                     @Param("queryEmbedding") String queryEmbedding,
                                     @Param("topK") int topK);

    @Select("SELECT kc.* FROM knowledge_chunk kc " +
            "ORDER BY kc.embedding <=> #{embedding}::vector LIMIT #{topK}")
    List<KnowledgeChunk> findSimilarGlobal(@Param("embedding") String embedding,
                                           @Param("topK") int topK);
}
