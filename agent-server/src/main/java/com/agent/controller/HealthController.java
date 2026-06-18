package com.agent.controller;

import cn.hutool.core.map.MapUtil;
import com.agent.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(MapUtil.<String, Object>builder()
                .put("status", "UP")
                .put("app", "agent-ai")
                .put("profile", activeProfile)
                .build());
    }
}
