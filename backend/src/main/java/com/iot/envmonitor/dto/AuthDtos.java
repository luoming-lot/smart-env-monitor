package com.iot.envmonitor.dto;

import com.iot.envmonitor.entity.User;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password
    ) {
    }

    public record UserInfo(Long id, String username, String nickname, String role) {
        public static UserInfo of(User user) {
            return new UserInfo(user.getId(), user.getUsername(), user.getNickname(), user.getRole());
        }
    }

    public record LoginResponse(String token, UserInfo user) {
    }
}
