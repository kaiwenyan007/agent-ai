package com.agent.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.agent.common.BusinessException;
import com.agent.dto.AppendMessageRequest;
import com.agent.dto.ConversationResponse;
import com.agent.dto.CreateConversationRequest;
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
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final Set<String> ALLOWED_ROLES = Set.of("user", "assistant", "system");

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    public List<ConversationResponse> listCurrentUserConversations() {
        Long userId = currentUserId();
        List<Conversation> conversations = conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUserId, userId)
                        .orderByDesc(Conversation::getUpdatedAt)
        );
        return conversations.stream().map(this::toConversationResponse).toList();
    }

    public ConversationResponse createConversation(CreateConversationRequest request) {
        Conversation conversation = new Conversation();
        conversation.setUserId(currentUserId());
        String title = request == null ? null : StrUtil.trim(request.getTitle());
        conversation.setTitle(StrUtil.isBlank(title) ? Conversation.DEFAULT_TITLE : title);
        conversationMapper.insert(conversation);
        return toConversationResponse(conversation);
    }

    public void deleteConversation(Long conversationId) {
        Conversation conversation = requireOwnedConversation(conversationId);
        conversationMapper.deleteById(conversation.getId());
    }

    public List<MessageResponse> listMessages(Long conversationId) {
        requireOwnedConversation(conversationId);
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByAsc(Message::getCreatedAt)
        );
        return messages.stream().map(this::toMessageResponse).toList();
    }

    @Transactional
    public MessageResponse appendMessage(Long conversationId, AppendMessageRequest request) {
        Conversation conversation = requireOwnedConversation(conversationId);
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

    static String buildTitleFromPrompt(String prompt) {
        String trimmed = StrUtil.trim(prompt);
        if (trimmed.length() <= 30) {
            return trimmed;
        }
        return StrUtil.subPre(trimmed, 30) + "\u2026";
    }

    private Conversation requireOwnedConversation(Long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException("会话不存在");
        }
        if (!conversation.getUserId().equals(currentUserId())) {
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
