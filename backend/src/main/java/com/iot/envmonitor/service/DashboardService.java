package com.iot.envmonitor.service;

import com.iot.envmonitor.dto.TelemetryDtos.LatestReading;
import com.iot.envmonitor.dto.TelemetryDtos.SummaryResponse;
import com.iot.envmonitor.entity.Device;
import com.iot.envmonitor.entity.SensorData;
import com.iot.envmonitor.repository.AlarmRepository;
import com.iot.envmonitor.repository.DeviceRepository;
import com.iot.envmonitor.repository.SensorDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DeviceRepository deviceRepository;
    private final SensorDataRepository sensorDataRepository;
    private final AlarmRepository alarmRepository;

    public SummaryResponse summary() {
        List<Device> devices = deviceRepository.findAll();
        long onlineCount = devices.stream()
                .filter(d -> d.getLastSeen() != null
                        && d.getLastSeen().isAfter(LocalDateTime.now().minusSeconds(DeviceService.ONLINE_TIMEOUT_SECONDS)))
                .count();
        List<Object[]> rows = sensorDataRepository.findAverageSince(LocalDateTime.now().minusHours(1));
        Object[] avg = rows.isEmpty() ? null : rows.get(0);
        Double avgTemperature = avg == null || avg[0] == null ? null : ((Number) avg[0]).doubleValue();
        Double avgHumidity = avg == null || avg[1] == null ? null : ((Number) avg[1]).doubleValue();
        return new SummaryResponse(
                devices.size(),
                onlineCount,
                alarmRepository.countByStatus("OPEN"),
                avgTemperature,
                avgHumidity,
                realtime());
    }

    public List<LatestReading> realtime() {
        Map<String, Device> deviceById = deviceRepository.findAll().stream()
                .collect(Collectors.toMap(Device::getId, Function.identity()));
        return sensorDataRepository.findLatestPerDevice().stream()
                .sorted(Comparator.comparing(SensorData::getTs).reversed())
                .map(s -> {
                    Device d = deviceById.get(s.getDeviceId());
                    return new LatestReading(s.getDeviceId(),
                            d == null ? s.getDeviceId() : d.getName(),
                            s.getTemperature(), s.getHumidity(), s.getRssi(), s.getBattery(), s.getTs());
                })
                .toList();
    }
}
