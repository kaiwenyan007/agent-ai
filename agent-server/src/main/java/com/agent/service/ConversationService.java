package com.agent.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.agent.common.BusinessException;
import com.agent.dto.AppendMessageRequest;
import com.agent.dto.ConversationResponse;
import com.agent.dto.CreateConversationRequest;
import com.agent.dto.MessagePageResponse;
import com.agent.dto.MessageResponse;
import com.agent.entity.Conversation;
import com.agent.entity.Message;
import com.agent.mapper.ConversationMapper;
import com.agent.mapper.MessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 会话与消息的持久化及归属校验。
 */
@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final Set<String> ALLOWED_ROLES = Set.of("user", "assistant", "system");

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    /**
     * 列出当前登录用户的全部会话。
     */
    public List<ConversationResponse> listCurrentUserConversations() {
        Long userId = currentUserId();
        List<Conversation> conversations = conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUserId, userId)
                        .orderByDesc(Conversation::getUpdatedAt)
        );
        return conversations.stream().map(this::toConversationResponse).toList();
    }

    /**
     * 创建空会话。
     *
     * @param request 可为 null；title 为空时使用 {@link Conversation#DEFAULT_TITLE}
     */
    public ConversationResponse createConversation(CreateConversationRequest request) {
        Conversation conversation = new Conversation();
        conversation.setUserId(currentUserId());
        String title = request == null ? null : StrUtil.trim(request.getTitle());
        conversation.setTitle(StrUtil.isBlank(title) ? Conversation.DEFAULT_TITLE : title);
        conversationMapper.insert(conversation);
        return toConversationResponse(conversation);
    }

    /**
     * 删除会话（逻辑删除），须为当前用户所有。
     */
    public void deleteConversation(Long conversationId) {
        Conversation conversation = requireOwnedConversation(conversationId);
        conversationMapper.deleteById(conversation.getId());
    }

    /** 默认每页条数 */
    public static final int DEFAULT_MESSAGE_PAGE_SIZE = 30;

    /**
     * 分页加载消息（聊天区滚动分页）。
     * <ul>
     *   <li>{@code beforeId=null}：加载最新一页</li>
     *   <li>{@code beforeId=xxx}：加载 id 更早的一页，用于上滑加载历史</li>
     * </ul>
     */
    public MessagePageResponse listMessagesPage(Long conversationId, Integer limit, Long beforeId) {
        requireOwnedConversation(conversationId);
        int pageSize = limit == null || limit <= 0 ? DEFAULT_MESSAGE_PAGE_SIZE : Math.min(limit, 100);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId);
        if (beforeId != null) {
            wrapper.lt(Message::getId, beforeId);
        }
        wrapper.orderByDesc(Message::getId).last("LIMIT " + (pageSize + 1));

        List<Message> batch = new ArrayList<>(messageMapper.selectList(wrapper));
        boolean hasMore = batch.size() > pageSize;
        if (hasMore) {
            batch = batch.subList(0, pageSize);
        }
        Collections.reverse(batch);

        return new MessagePageResponse(
                batch.stream().map(this::toMessageResponse).toList(),
                hasMore
        );
    }

    /**
     * 按时间正序返回会话内全部消息。
     */
    public List<MessageResponse> listMessages(Long conversationId) {
        requireOwnedConversation(conversationId);
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByAsc(Message::getCreatedAt)
        );
        return messages.stream().map(this::toMessageResponse).toList();
    }

    /**
     * 追加一条消息，并刷新会话 {@code updated_at}。
     * <p>
     * 首条 user 消息会自动将会话标题设为 prompt 前 30 字（对标 agent-demo）。
     */
    @Transactional
    public MessageResponse appendMessage(Long conversationId, AppendMessageRequest request) {
        return appendMessage(currentUserId(), conversationId, request);
    }

    /**
     * 指定 userId 追加消息，供异步线程（无 Sa-Token Web 上下文）调用。
     */
    @Transactional
    public MessageResponse appendMessage(Long userId, Long conversationId, AppendMessageRequest request) {
        Conversation conversation = requireOwnedConversation(conversationId, userId);
        String content = StrUtil.trim(request.getContent());
        if (StrUtil.isBlank(content)) {
            throw new BusinessException("消息内容不能为空");
        }

        String role = StrUtil.blankToDefault(StrUtil.trim(request.getRole()), "user");
        if (!ALLOWED_ROLES.contains(role)) {
            throw new BusinessException("不支持的消息角色: " + role);
        }

        Message message = new Message();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        messageMapper.insert(message);

        maybeUpdateTitle(conversation, role, content);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);

        return toMessageResponse(message);
    }

    /**
     * 仅当会话仍为默认标题且这是第一条 user 消息时，用 prompt 生成标题。
     */
    private void maybeUpdateTitle(Conversation conversation, String role, String content) {
        if (!"user".equals(role)) {
            return;
        }
        if (!Conversation.DEFAULT_TITLE.equals(conversation.getTitle())) {
            return;
        }
        Long userMessageCount = messageMapper.selectCount(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversation.getId())
                        .eq(Message::getRole, "user")
        );
        if (userMessageCount != 1) {
            return;
        }
        conversation.setTitle(buildTitleFromPrompt(content));
    }

    /**
     * 截取 prompt 前 30 字符作为会话标题，超出追加省略号。
     */
    static String buildTitleFromPrompt(String prompt) {
        String trimmed = StrUtil.trim(prompt);
        if (trimmed.length() <= 30) {
            return trimmed;
        }
        return StrUtil.subPre(trimmed, 30) + "\u2026";
    }

    /** 校验会话存在且归属指定用户（Web 请求线程使用当前登录用户）。 */
    private Conversation requireOwnedConversation(Long conversationId) {
        return requireOwnedConversation(conversationId, currentUserId());
    }

    /** 校验会话存在且归属指定用户。 */
    private Conversation requireOwnedConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException("会话不存在");
        }
        if (!conversation.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该会话");
        }
        return conversation;
    }

    private long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    private ConversationResponse toConversationResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    private MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
