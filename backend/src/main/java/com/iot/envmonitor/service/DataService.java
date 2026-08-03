package com.iot.envmonitor.service;

import com.iot.envmonitor.dto.TelemetryDtos.HistoryPoint;
import com.iot.envmonitor.entity.SensorData;
import com.iot.envmonitor.repository.SensorDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DataService {

    private final SensorDataRepository sensorDataRepository;

    /**
     * 查询历史数据；intervalSeconds 为聚合窗口，<=0 时按时间跨度自动选择。
     * ponytail: 当前在内存中聚合，适合作品集/中小规模；数据量增大后应改为
     * 数据库侧聚合或引入时序数据库（TimescaleDB / InfluxDB）。
     */
    public List<HistoryPoint> history(String deviceId, LocalDateTime start, LocalDateTime end, int intervalSeconds) {
        if (start == null) {
            start = LocalDateTime.now().minusHours(24);
        }
        if (end == null) {
            end = LocalDateTime.now();
        }
        if (intervalSeconds <= 0) {
            intervalSeconds = (int) Math.max(60, Duration.between(start, end).toSeconds() / 240);
        }
        List<SensorData> rows = sensorDataRepository
                .findByDeviceIdAndTsBetweenOrderByTsAsc(deviceId, start, end);

        Map<Long, Bucket> buckets = new LinkedHashMap<>();
        for (SensorData row : rows) {
            long epoch = row.getTs().toEpochSecond(ZoneOffset.UTC);
            long bucketKey = epoch - Math.floorMod(epoch, intervalSeconds);
            buckets.computeIfAbsent(bucketKey, k -> new Bucket())
                    .add(row.getTemperature(), row.getHumidity());
        }
        List<HistoryPoint> points = new ArrayList<>(buckets.size());
        for (Map.Entry<Long, Bucket> entry : buckets.entrySet()) {
            Bucket b = entry.getValue();
            points.add(new HistoryPoint(
                    LocalDateTime.ofEpochSecond(entry.getKey(), 0, ZoneOffset.UTC),
                    b.sumTemp / b.count,
                    b.sumHumidity / b.count));
        }
        return points;
    }

    private static class Bucket {
        double sumTemp;
        double sumHumidity;
        int count;

        void add(double temperature, double humidity) {
            sumTemp += temperature;
            sumHumidity += humidity;
            count++;
        }
    }
}
