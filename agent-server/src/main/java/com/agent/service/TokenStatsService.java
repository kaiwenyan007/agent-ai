package com.agent.service;

import cn.dev33.satoken.stp.StpUtil;
import com.agent.dto.TokenSummaryResponse;
import com.agent.entity.TokenUsage;
import com.agent.mapper.TokenUsageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Token 用量聚合统计（数据来源：{@code token_usage} 表）。
 */
@Service
@RequiredArgsConstructor
public class TokenStatsService {

    private final TokenUsageMapper tokenUsageMapper;

    /**
     * 汇总当前用户全部 Token 记录，并返回最近 20 条明细。
     */
    public TokenSummaryResponse getCurrentUserSummary() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<TokenUsage> records = tokenUsageMapper.selectList(
                new LambdaQueryWrapper<TokenUsage>()
                        .eq(TokenUsage::getUserId, userId)
                        .orderByDesc(TokenUsage::getCreatedAt)
        );

        long prompt = 0;
        long completion = 0;
        long total = 0;
        BigDecimal cost = BigDecimal.ZERO;
        for (TokenUsage record : records) {
            prompt += safe(record.getPromptTokens());
            completion += safe(record.getCompletionTokens());
            total += safe(record.getTotalTokens());
            if (record.getEstimatedCost() != null) {
                cost = cost.add(record.getEstimatedCost());
            }
        }

        List<TokenSummaryResponse.RecentUsageItem> recent = records.stream()
                .sorted(Comparator.comparing(TokenUsage::getCreatedAt).reversed())
                .limit(20)
                .map(record -> new TokenSummaryResponse.RecentUsageItem(
                        record.getId(),
                        record.getModel(),
                        safe(record.getTotalTokens()),
                        record.getEstimatedCost() == null ? BigDecimal.ZERO : record.getEstimatedCost(),
                        record.getCreatedAt()
                ))
                .toList();

        return new TokenSummaryResponse(total, prompt, completion, cost, recent);
    }

    private static int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
