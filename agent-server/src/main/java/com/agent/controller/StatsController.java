package com.agent.controller;

import com.agent.common.ApiResponse;
import com.agent.dto.TokenSummaryResponse;
import com.agent.service.TokenStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用量统计接口。
 * <p>
 * 数据来源于 {@code token_usage} 表，在流式对话完成后写入。
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final TokenStatsService tokenStatsService;

    /**
     * 当前用户 Token 汇总与最近 20 条使用记录。
     */
    @GetMapping("/tokens")
    public ApiResponse<TokenSummaryResponse> tokenSummary() {
        return ApiResponse.ok(tokenStatsService.getCurrentUserSummary());
    }
}
