package com.agent.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_api_configs")
public class UserApiConfig {

    @TableId("user_id")
    private Long userId;

    @TableField("api_key")
    private String apiKey;

    @TableField("base_url")
    private String baseUrl;

    private String model;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
