package org.example.web.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter("/*")
public class DynamicPageCacheControlFilter implements Filter {

    @Override
    public void doFilter(
            jakarta.servlet.ServletRequest request,
            jakarta.servlet.ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestPath = httpRequest.getRequestURI()
                .substring(httpRequest.getContextPath().length());
        if (requestPath.startsWith("/static/")) {
            chain.doFilter(request, response);
            return;
        }

        if (isSensitivePath(requestPath)) {
            httpResponse.setHeader(
                    "Cache-Control",
                    "no-store, no-cache, must-revalidate, max-age=0"
            );
            httpResponse.setHeader("Pragma", "no-cache");
            httpResponse.setDateHeader("Expires", 0);
        } else {
            httpResponse.setHeader(
                    "Cache-Control",
                    "no-cache, max-age=0, must-revalidate"
            );
        }

        chain.doFilter(request, response);
    }

    private boolean isSensitivePath(String requestPath) {
        return requestPath.equals("/admin")
                || requestPath.startsWith("/member/")
                || requestPath.startsWith("/admin/")
                || requestPath.startsWith("/auth/");
    }
}
