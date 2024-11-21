package com.huaixv06.fileCenter.aop;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/*")  // 对所有请求进行过滤
public class SameSiteCookieFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // Filter 初始化时执行的代码
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 在响应头中添加 SameSite=None，确保跨域请求时携带 Cookie
        httpResponse.setHeader("Set-Cookie", "SameSite=None; Secure");

        // 继续执行后续的过滤链
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 资源清理代码
    }
}
