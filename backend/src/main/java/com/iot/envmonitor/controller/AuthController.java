package com.iot.envmonitor.controller;

import com.iot.envmonitor.dto.AuthDtos.LoginRequest;
import com.iot.envmonitor.dto.AuthDtos.LoginResponse;
import com.iot.envmonitor.dto.AuthDtos.UserInfo;
import com.iot.envmonitor.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserInfo me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}
