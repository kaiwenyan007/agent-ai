package com.agent.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 路由鉴权：除 register / login / health 外，{@code /api/**} 均需登录。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> SaRouter
                        .match("/api/**")
                        .notMatch("/api/auth/register", "/api/auth/login", "/api/health")
                        .notMatchMethod("OPTIONS")
                        .check(r -> StpUtil.checkLogin())))
                .addPathPatterns("/api/**");
    }
}
