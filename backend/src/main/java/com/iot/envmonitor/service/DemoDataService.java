package com.iot.envmonitor.service;

import com.iot.envmonitor.dto.TelemetryDtos.TelemetryReading;
import com.iot.envmonitor.entity.Device;
import com.iot.envmonitor.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * 在线演示数据模拟器：在没有真实硬件/MQTT 的环境（如 Render 免费档）下，
 * 每 5 秒生成 3 台演示设备的遥测数据，走与 MQTT 相同的采集链路，
 * 因此报警、实时推送、历史曲线等行为与真实设备完全一致。
 * 由 app.demo-data.enabled 控制（demo profile 默认开启）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataService {

    private final TelemetryService telemetryService;
    private final DeviceRepository deviceRepository;
    private final Random random = new Random();

    private static final List<String[]> DEMO_DEVICES = List.of(
            new String[]{"esp32-demo-001", "演示设备 A（实验室）", "实验室 201"},
            new String[]{"esp32-demo-002", "演示设备 B（机房）", "机房 102"},
            new String[]{"esp32-demo-003", "演示设备 C（仓库）", "一号仓库"});

    @Scheduled(fixedDelay = 5000)
    public void simulate() {
        double t = LocalDateTime.now().getSecond() / 60.0 * 2 * Math.PI;
        for (int i = 0; i < DEMO_DEVICES.size(); i++) {
            String[] device = DEMO_DEVICES.get(i);
            ensureDevice(device[0], device[1], device[2]);
            double temperature = 24 + i * 2 + 2.5 * Math.sin(t + i);
            double humidity = 52 + i * 4 + 6 * Math.cos(t * 0.8 + i);
            if (random.nextDouble() < 0.04) {
                temperature += 20; // 偶发高温尖峰（超过默认 45°C 阈值），演示报警触发与恢复
            }
            TelemetryReading reading = new TelemetryReading(
                    Math.round(temperature * 10) / 10.0,
                    Math.round(Math.max(0, Math.min(100, humidity)) * 10) / 10.0,
                    -50 - random.nextInt(25),
                    70 + random.nextInt(31),
                    "1.0.0-demo");
            try {
                telemetryService.ingest(device[0], reading);
            } catch (Exception e) {
                log.warn("演示数据生成失败: {}", e.getMessage());
            }
        }
    }

    private void ensureDevice(String id, String name, String location) {
        deviceRepository.findById(id).orElseGet(() -> {
            Device device = new Device();
            device.setId(id);
            device.setName(name);
            device.setLocation(location);
            return deviceRepository.save(device);
        });
    }
}
