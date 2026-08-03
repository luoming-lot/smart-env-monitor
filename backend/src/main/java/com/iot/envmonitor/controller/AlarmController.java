package com.iot.envmonitor.controller;

import com.iot.envmonitor.entity.Alarm;
import com.iot.envmonitor.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alarms")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    @GetMapping
    public Page<Alarm> list(@RequestParam(required = false) String status,
                            @RequestParam(required = false) String deviceId,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size) {
        return alarmService.list(status, deviceId,
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "triggeredAt")));
    }

    @PutMapping("/{id}/resolve")
    public Alarm resolve(@PathVariable Long id, Authentication authentication) {
        return alarmService.resolve(id, authentication.getName());
    }
}
