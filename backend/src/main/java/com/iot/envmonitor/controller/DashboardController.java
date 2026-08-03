package com.iot.envmonitor.controller;

import com.iot.envmonitor.dto.TelemetryDtos.SummaryResponse;
import com.iot.envmonitor.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public SummaryResponse summary() {
        return dashboardService.summary();
    }

    @GetMapping("/realtime")
    public java.util.List<com.iot.envmonitor.dto.TelemetryDtos.LatestReading> realtime() {
        return dashboardService.realtime();
    }
}
