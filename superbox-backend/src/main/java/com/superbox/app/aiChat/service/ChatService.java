package com.superbox.app.aiChat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superbox.app.aiChat.dto.ChatStreamEvent;
import com.superbox.app.aiChat.entity.ChatMessage;
import com.superbox.app.aiChat.entity.Conversation;
import com.superbox.app.aiChat.mapper.ChatMessageMapper;
import com.superbox.app.aiChat.mapper.ConversationMapper;
import com.superbox.app.knowledgeBase.mapper.KnowledgeChunkMapper;
import com.superbox.app.knowledgeBase.service.ChunkingService;
import com.superbox.app.knowledgeBase.service.EmbeddingService;
import com.superbox.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationMapper convMapper;
    private final ChatMessageMapper msgMapper;
    private final ModelRouterService modelRouter;
    private final KnowledgeChunkMapper chunkMapper;
    private final EmbeddingService embeddingService;
    private final ChunkingService chunkingService;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public List<Conversation> listConversations() {
        return convMapper.findAll();
    }

    public Conversation getConversation(Long id) {
        Conversation conv = convMapper.findById(id);
        if (conv == null) throw new BusinessException(404, "对话不存在");
        return conv;
    }

    public List<ChatMessage> getMessages(Long conversationId) {
        return msgMapper.findByConversationId(conversationId);
    }

    @Transactional
    public Conversation createConversation(String title, String model) {
        Conversation conv = new Conversation();
        conv.setTitle(title != null ? title : "新对话");
        conv.setModel(model != null ? model : "gpt-4o-mini");
        conv.setMessageCount(0);
        convMapper.insert(conv);
        return conv;
    }

    @Transactional
    public void deleteConversation(Long id) {
        getConversation(id);
        msgMapper.deleteByConversationId(id);
        convMapper.delete(id);
    }

    public SseEmitter streamChat(Long conversationId, String prompt, String model,
                                  boolean reasoningEnabled, boolean ragEnabled) {
        SseEmitter emitter = new SseEmitter(300000L);

        executor.execute(() -> {
            try {
                Conversation conv = getConversation(conversationId);

                String systemPrompt = null;
                List<ChatStreamEvent.SourceRef> sources = null;
                if (ragEnabled) {
                    var ragResult = buildRagContext(prompt);
                    if (ragResult != null && !ragResult.sources().isEmpty()) {
                        sources = ragResult.sources();
                        systemPrompt = "请参考以下知识库内容回答问题：\n\n" + ragResult.context() +
                                "\n\n---\n如果知识库内容与问题无关，请基于你的知识如实回答。";
                        sendEvent(emitter, new ChatStreamEvent("sources", null, sources, null, null));
                    }
                }

                String actualModel = model != null ? model : conv.getModel();
                if (reasoningEnabled && actualModel.equals("deepseek-chat")) {
                    actualModel = "deepseek-reasoner";
                }

                ChatMessage userMsg = new ChatMessage();
                userMsg.setConversationId(conversationId);
                userMsg.setRole("user");
                userMsg.setContent(prompt);
                userMsg.setModel(actualModel);
                msgMapper.insert(userMsg);

                List<ChatMessage> history = msgMapper.findRecent(conversationId, 30);
                Collections.reverse(history);

                StringBuilder reasoningBuf = new StringBuilder();
                StringBuilder answerBuf = new StringBuilder();

                var provider = modelRouter.route(actualModel);
                provider.chatStream(systemPrompt, history, actualModel, event -> {
                    try {
                        if ("reasoning".equals(event.getType())) {
                            reasoningBuf.append(event.getDelta() != null ? event.getDelta() : "");
                        } else if ("answer".equals(event.getType())) {
                            answerBuf.append(event.getDelta() != null ? event.getDelta() : "");
                        }
                        sendEvent(emitter, event);
                    } catch (IOException e) {
                        log.error("SSE send error", e);
                    }
                });

                ChatMessage assistantMsg = new ChatMessage();
                assistantMsg.setConversationId(conversationId);
                assistantMsg.setRole("assistant");
                assistantMsg.setContent(answerBuf.toString());
                assistantMsg.setReasoningContent(reasoningBuf.length() > 0 ? reasoningBuf.toString() : null);
                if (sources != null && !sources.isEmpty()) {
                    assistantMsg.setSources(jsonMapper.writeValueAsString(sources));
                }
                assistantMsg.setTokenCount(reasoningBuf.length() + answerBuf.length());
                assistantMsg.setModel(actualModel);
                msgMapper.insert(assistantMsg);

                conv.setMessageCount((conv.getMessageCount() != null ? conv.getMessageCount() : 0) + 1);
                convMapper.update(conv);

                emitter.complete();
            } catch (Exception e) {
                log.error("Chat stream error", e);
                try {
                    sendEvent(emitter, new ChatStreamEvent("error", e.getMessage(), null, null, null));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        emitter.onCompletion(() -> log.debug("SSE completed"));
        emitter.onTimeout(() -> log.debug("SSE timeout"));
        return emitter;
    }

    private RAGResult buildRagContext(String query) {
        try {
            List<Float> queryEmbedding = embeddingService.embed(query);
            String vectorStr = embeddingService.toPgVectorString(queryEmbedding);
            var chunks = chunkMapper.findSimilarGlobal(vectorStr, 5);
            if (chunks.isEmpty()) return null;

            StringBuilder ctx = new StringBuilder();
            var sources = new java.util.ArrayList<ChatStreamEvent.SourceRef>();
            for (int i = 0; i < chunks.size(); i++) {
                var chunk = chunks.get(i);
                ctx.append("【资料").append(i + 1).append("】").append(chunk.getContent()).append("\n\n");
                sources.add(new ChatStreamEvent.SourceRef(chunk.getEntryId(), "条目#" + chunk.getEntryId(), 0.9f - i * 0.05f));
            }
            return new RAGResult(ctx.toString(), sources);
        } catch (Exception e) {
            log.warn("RAG context build failed", e);
            return null;
        }
    }

    private record RAGResult(String context, List<ChatStreamEvent.SourceRef> sources) {}

    private void sendEvent(SseEmitter emitter, ChatStreamEvent event) throws IOException {
        emitter.send(SseEmitter.event()
                .name(event.getType())
                .data(jsonMapper.writeValueAsString(event)));
    }
}
