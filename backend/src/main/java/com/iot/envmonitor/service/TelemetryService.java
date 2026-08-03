package com.iot.envmonitor.service;

import com.iot.envmonitor.common.ApiException;
import com.iot.envmonitor.dto.TelemetryDtos.LatestReading;
import com.iot.envmonitor.dto.TelemetryDtos.TelemetryReading;
import com.iot.envmonitor.entity.Device;
import com.iot.envmonitor.entity.SensorData;
import com.iot.envmonitor.repository.DeviceRepository;
import com.iot.envmonitor.repository.SensorDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 遥测数据入口：MQTT 消息解析后统一走这里，
 * 完成设备登记、落库、报警判定和 WebSocket 实时推送。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryService {

    private final DeviceRepository deviceRepository;
    private final SensorDataRepository sensorDataRepository;
    private final DeviceService deviceService;
    private final AlarmService alarmService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void ingest(String deviceId, TelemetryReading reading) {
        if (!reading.isValid()) {
            throw ApiException.badRequest("遥测数据非法: " + reading);
        }
        Device device = deviceRepository.findById(deviceId)
                .orElseGet(() -> {
                    log.info("自动注册新设备: {}", deviceId);
                    return deviceService.register(deviceId, reading.firmwareVersion());
                });
        if (reading.firmwareVersion() != null && !reading.firmwareVersion().equals(device.getFirmwareVersion())) {
            device.setFirmwareVersion(reading.firmwareVersion());
        }
        device.setLastSeen(LocalDateTime.now());
        deviceRepository.save(device);

        SensorData data = new SensorData();
        data.setDeviceId(deviceId);
        data.setTemperature(reading.temperature());
        data.setHumidity(reading.humidity());
        data.setRssi(reading.rssi());
        data.setBattery(reading.battery());
        data.setTs(LocalDateTime.now());
        sensorDataRepository.save(data);

        alarmService.evaluateAndPersist(device, reading.temperature(), reading.humidity());

        LatestReading payload = new LatestReading(
                deviceId, device.getName(),
                reading.temperature(), reading.humidity(), reading.rssi(), reading.battery(),
                data.getTs());
        messagingTemplate.convertAndSend("/topic/telemetry", payload);
        messagingTemplate.convertAndSend("/topic/devices/" + deviceId + "/data", payload);
    }
}
