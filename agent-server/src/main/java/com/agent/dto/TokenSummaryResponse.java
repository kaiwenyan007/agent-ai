package com.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** GET /api/stats/tokens 响应 data */
@Data
@AllArgsConstructor
public class TokenSummaryResponse {

    private long totalTokens;

    private long promptTokens;

    private long completionTokens;

    private BigDecimal estimatedCost;

    /** 最近 20 条使用记录 */
    private List<RecentUsageItem> recent;

    @Data
    @AllArgsConstructor
    public static class RecentUsageItem {

        private Long id;

        private String model;

        private int totalTokens;

        private BigDecimal estimatedCost;

        private LocalDateTime createdAt;
    }
}
