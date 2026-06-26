package com.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** GET/POST /api/settings/models 响应 data */
@Data
@AllArgsConstructor
public class ModelsResponse {

    private List<String> models;

    /** true 表示来自远程 /models 接口；false 为本地兜底列表 */
    private boolean fromRemote;
}
