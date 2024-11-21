package com.huaixv06.fileCenter.config;

import com.huaixv06.fileCenter.aop.SameSiteCookieFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<SameSiteCookieFilter> sameSiteCookieFilter() {
        FilterRegistrationBean<SameSiteCookieFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new SameSiteCookieFilter());
        registrationBean.addUrlPatterns("/api/*");  // 如果只想对某些路径有效，您可以添加 URL 路径
        return registrationBean;
    }
}
