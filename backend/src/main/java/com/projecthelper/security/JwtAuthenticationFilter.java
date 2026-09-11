package com.projecthelper.security;

import com.projecthelper.user.User;
import com.projecthelper.user.UserRepository;
import com.projecthelper.user.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                // JWT 只携带用户 ID；每次请求仍从数据库读取用户，以便立即识别禁用账户。
                String userId = jwtService.parseUserId(authorization.substring(7));
                User user = userRepository.findById(userId).orElse(null);
                if (user != null && user.getStatus() == UserStatus.ACTIVE) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            user.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) {
                // 无效或过期令牌按未登录处理，由 Spring Security 返回 401/403。
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
