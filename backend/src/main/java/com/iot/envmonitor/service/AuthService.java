package com.iot.envmonitor.service;

import com.iot.envmonitor.common.ApiException;
import com.iot.envmonitor.dto.AuthDtos.LoginRequest;
import com.iot.envmonitor.dto.AuthDtos.LoginResponse;
import com.iot.envmonitor.dto.AuthDtos.UserInfo;
import com.iot.envmonitor.entity.User;
import com.iot.envmonitor.repository.UserRepository;
import com.iot.envmonitor.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> ApiException.unauthorized("用户名或密码错误"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw ApiException.unauthorized("用户名或密码错误");
        }
        return new LoginResponse(jwtService.generate(user), UserInfo.of(user));
    }

    public UserInfo me(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("用户不存在"));
        return UserInfo.of(user);
    }
}
