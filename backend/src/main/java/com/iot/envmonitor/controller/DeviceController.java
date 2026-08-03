package com.iot.envmonitor.controller;

import com.iot.envmonitor.dto.DeviceDtos.DeviceInfo;
import com.iot.envmonitor.dto.DeviceDtos.DeviceRequest;
import com.iot.envmonitor.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public List<DeviceInfo> list(@RequestParam(required = false) String keyword) {
        return deviceService.list(keyword);
    }

    @GetMapping("/{id}")
    public DeviceInfo get(@PathVariable String id) {
        return deviceService.get(id);
    }

    @PostMapping
    public DeviceInfo create(@Valid @RequestBody DeviceRequest request) {
        return deviceService.create(request);
    }

    @PutMapping("/{id}")
    public DeviceInfo update(@PathVariable String id, @Valid @RequestBody DeviceRequest request) {
        return deviceService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        deviceService.delete(id);
    }
}
