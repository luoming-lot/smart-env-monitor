package com.iot.envmonitor.controller;

import com.iot.envmonitor.dto.TelemetryDtos.HistoryPoint;
import com.iot.envmonitor.service.DataService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DataController {

    private final DataService dataService;

    @GetMapping("/devices/{id}/data")
    public List<HistoryPoint> history(
            @PathVariable String id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int interval) {
        return dataService.history(id, start, end, interval);
    }
}
