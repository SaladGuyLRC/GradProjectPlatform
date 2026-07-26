package com.projecthelper.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestIdFilter extends OncePerRequestFilter {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    public static String currentRequestId() {
        return CURRENT.get();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) requestId = UUID.randomUUID().toString();
        CURRENT.set(requestId);
        response.setHeader("X-Request-Id", requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            CURRENT.remove();
        }
    }
}
