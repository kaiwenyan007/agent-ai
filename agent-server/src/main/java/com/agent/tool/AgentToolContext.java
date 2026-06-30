package com.agent.tool;

/**
 * 当前对话请求的工具上下文（userId / conversationId），供 {@link AgentTools} 读取。
 * <p>
 * 对标 Python {@code agent.rag_context}，在 SSE 流式对话线程内设置与清理。
 */
public final class AgentToolContext {

    public record Context(Long userId, Long conversationId) {}

    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    private AgentToolContext() {}

    public static Context get() {
        return HOLDER.get();
    }

    public static Long currentUserId() {
        Context ctx = HOLDER.get();
        return ctx == null ? null : ctx.userId();
    }

    public static void set(Long userId, Long conversationId) {
        HOLDER.set(new Context(userId, conversationId));
    }

    public static void clear() {
        HOLDER.remove();
    }
}
