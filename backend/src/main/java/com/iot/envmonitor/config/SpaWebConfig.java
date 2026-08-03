package com.iot.envmonitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 前端打包进后端 jar 后，SPA 路由（/login、/devices 等）回退到 index.html。
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("forward:/index.html");
        registry.addViewController("/devices").setViewName("forward:/index.html");
        registry.addViewController("/history").setViewName("forward:/index.html");
        registry.addViewController("/alarms").setViewName("forward:/index.html");
    }
}
