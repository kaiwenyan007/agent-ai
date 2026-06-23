package com.agent.controller;

import com.agent.common.ApiResponse;
import com.agent.dto.TokenSummaryResponse;
import com.agent.service.TokenStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final TokenStatsService tokenStatsService;

    @GetMapping("/tokens")
    public ApiResponse<TokenSummaryResponse> tokenSummary() {
        return ApiResponse.ok(tokenStatsService.getCurrentUserSummary());
    }
}
