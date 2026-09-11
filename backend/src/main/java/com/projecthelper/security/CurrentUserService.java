package com.projecthelper.security;

import com.projecthelper.common.BusinessException;
import com.projecthelper.user.User;
import com.projecthelper.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User require() {
        // 控制器和服务统一从 Spring Security 上下文解析当前用户，避免客户端自行提交身份。
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Please log in first");
        }
        return userRepository.findById(authentication.getName())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "The authenticated user no longer exists"));
    }
}
