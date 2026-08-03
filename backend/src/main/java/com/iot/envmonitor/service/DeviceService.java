package com.iot.envmonitor.service;

import com.iot.envmonitor.common.ApiException;
import com.iot.envmonitor.dto.DeviceDtos.DeviceInfo;
import com.iot.envmonitor.dto.DeviceDtos.DeviceRequest;
import com.iot.envmonitor.entity.Device;
import com.iot.envmonitor.entity.SensorData;
import com.iot.envmonitor.repository.DeviceRepository;
import com.iot.envmonitor.repository.SensorDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceService {

    /** 超过该秒数未上报视为离线 */
    public static final long ONLINE_TIMEOUT_SECONDS = 120;

    private final DeviceRepository deviceRepository;
    private final SensorDataRepository sensorDataRepository;

    public List<DeviceInfo> list(String keyword) {
        List<Device> devices = deviceRepository.findAll();
        Map<String, SensorData> latestById = sensorDataRepository.findLatestPerDevice().stream()
                .collect(Collectors.toMap(SensorData::getDeviceId, Function.identity()));
        return devices.stream()
                .filter(d -> !StringUtils.hasText(keyword)
                        || d.getId().toLowerCase().contains(keyword.toLowerCase())
                        || (d.getName() != null && d.getName().toLowerCase().contains(keyword.toLowerCase())))
                .map(d -> toInfo(d, latestById.get(d.getId())))
                .toList();
    }

    public DeviceInfo get(String id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("设备不存在: " + id));
        SensorData latest = sensorDataRepository.findLatestPerDevice().stream()
                .filter(s -> s.getDeviceId().equals(id))
                .findFirst().orElse(null);
        return toInfo(device, latest);
    }

    @Transactional
    public DeviceInfo create(DeviceRequest request) {
        if (deviceRepository.existsById(request.id())) {
            throw ApiException.conflict("设备 ID 已存在: " + request.id());
        }
        Device device = new Device();
        device.setId(request.id());
        apply(device, request);
        return toInfo(deviceRepository.save(device), null);
    }

    @Transactional
    public DeviceInfo update(String id, DeviceRequest request) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("设备不存在: " + id));
        apply(device, request);
        SensorData latest = sensorDataRepository.findLatestPerDevice().stream()
                .filter(s -> s.getDeviceId().equals(id))
                .findFirst().orElse(null);
        return toInfo(deviceRepository.save(device), latest);
    }

    @Transactional
    public void delete(String id) {
        if (!deviceRepository.existsById(id)) {
            throw ApiException.notFound("设备不存在: " + id);
        }
        deviceRepository.deleteById(id);
        // 传感器历史数据保留，便于审计；如需要可在此级联删除
    }

    /** MQTT 首次上报时自动注册设备 */
    @Transactional
    public Device register(String deviceId, String firmwareVersion) {
        Device device = new Device();
        device.setId(deviceId);
        device.setName(deviceId);
        device.setFirmwareVersion(firmwareVersion);
        return deviceRepository.save(device);
    }

    private void apply(Device device, DeviceRequest request) {
        if (StringUtils.hasText(request.name())) {
            device.setName(request.name());
        }
        device.setLocation(request.location());
        if (request.temperatureMin() != null) {
            device.setTemperatureMin(request.temperatureMin());
        }
        if (request.temperatureMax() != null) {
            device.setTemperatureMax(request.temperatureMax());
        }
        if (request.humidityMin() != null) {
            device.setHumidityMin(request.humidityMin());
        }
        if (request.humidityMax() != null) {
            device.setHumidityMax(request.humidityMax());
        }
    }

    private DeviceInfo toInfo(Device device, SensorData latest) {
        boolean online = device.getLastSeen() != null
                && device.getLastSeen().isAfter(LocalDateTime.now().minusSeconds(ONLINE_TIMEOUT_SECONDS));
        return DeviceInfo.of(device, online,
                latest == null ? null : latest.getTemperature(),
                latest == null ? null : latest.getHumidity());
    }
}
