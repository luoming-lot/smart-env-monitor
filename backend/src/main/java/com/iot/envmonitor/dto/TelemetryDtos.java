package com.iot.envmonitor.dto;

import java.time.LocalDateTime;

public final class TelemetryDtos {

    private TelemetryDtos() {
    }

    /** MQTT 上报的遥测数据（JSON 载荷） */
    public record TelemetryReading(
            Double temperature,
            Double humidity,
            Integer rssi,
            Integer battery,
            String firmwareVersion
    ) {
        public boolean isValid() {
            return temperature != null && humidity != null
                    && temperature >= -40 && temperature <= 100
                    && humidity >= 0 && humidity <= 100;
        }
    }

    /** 推送给前端的实时读数 */
    public record LatestReading(
            String deviceId,
            String deviceName,
            Double temperature,
            Double humidity,
            Integer rssi,
            Integer battery,
            LocalDateTime ts
    ) {
    }

    /** 历史曲线上的一个采样点（已按时间窗口聚合） */
    public record HistoryPoint(LocalDateTime ts, double temperature, double humidity) {
    }

    public record SummaryResponse(
            long deviceCount,
            long onlineCount,
            long openAlarmCount,
            Double avgTemperature,
            Double avgHumidity,
            java.util.List<LatestReading> realtime
    ) {
    }
}
