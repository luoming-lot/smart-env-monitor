package com.iot.envmonitor.dto;

import com.iot.envmonitor.entity.Device;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public final class DeviceDtos {

    private DeviceDtos() {
    }

    public record DeviceRequest(
            @NotBlank(message = "设备 ID 不能为空") String id,
            String name,
            String location,
            Double temperatureMin,
            Double temperatureMax,
            Double humidityMin,
            Double humidityMax
    ) {
    }

    public record DeviceInfo(
            String id,
            String name,
            String location,
            String firmwareVersion,
            double temperatureMin,
            double temperatureMax,
            double humidityMin,
            double humidityMax,
            boolean online,
            LocalDateTime lastSeen,
            Double latestTemperature,
            Double latestHumidity,
            LocalDateTime createdAt
    ) {
        public static DeviceInfo of(Device d, boolean online, Double latestTemperature, Double latestHumidity) {
            return new DeviceInfo(
                    d.getId(), d.getName(), d.getLocation(), d.getFirmwareVersion(),
                    d.getTemperatureMin(), d.getTemperatureMax(), d.getHumidityMin(), d.getHumidityMax(),
                    online, d.getLastSeen(), latestTemperature, latestHumidity, d.getCreatedAt());
        }
    }
}
