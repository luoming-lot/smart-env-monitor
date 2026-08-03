package com.iot.envmonitor.service;

import com.iot.envmonitor.common.ApiException;
import com.iot.envmonitor.entity.Alarm;
import com.iot.envmonitor.entity.Device;
import com.iot.envmonitor.logic.AlarmEvaluator;
import com.iot.envmonitor.repository.AlarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AlarmService {

    private final AlarmRepository alarmRepository;

    public Page<Alarm> list(String status, String deviceId, Pageable pageable) {
        return alarmRepository.search(blankToNull(status), blankToNull(deviceId), pageable);
    }

    @Transactional
    public Alarm resolve(Long id, String username) {
        Alarm alarm = alarmRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("报警记录不存在: " + id));
        if (!Alarm.STATUS_OPEN.equals(alarm.getStatus())) {
            return alarm;
        }
        alarm.setStatus(Alarm.STATUS_RESOLVED);
        alarm.setResolvedAt(LocalDateTime.now());
        alarm.setResolvedBy(username);
        return alarmRepository.save(alarm);
    }

    /** 上报数据后判定并持久化报警：超阈值生成 OPEN 报警，恢复区间自动闭环 */
    @Transactional
    public void evaluateAndPersist(Device device, double temperature, double humidity) {
        List<AlarmEvaluator.AlarmEvent> events = AlarmEvaluator.evaluate(device, temperature, humidity);
        if (events.isEmpty()) {
            autoResolveAll(device.getId());
            return;
        }
        for (AlarmEvaluator.AlarmEvent event : events) {
            boolean alreadyOpen = alarmRepository
                    .findFirstByDeviceIdAndTypeAndStatus(device.getId(), event.type(), Alarm.STATUS_OPEN)
                    .isPresent();
            if (!alreadyOpen) {
                Alarm alarm = new Alarm();
                alarm.setDeviceId(device.getId());
                alarm.setType(event.type());
                alarm.setValue(event.value());
                alarm.setThreshold(event.threshold());
                alarm.setMessage(event.message());
                alarm.setStatus(Alarm.STATUS_OPEN);
                alarm.setTriggeredAt(LocalDateTime.now());
                alarmRepository.save(alarm);
            }
        }
    }

    private void autoResolveAll(String deviceId) {
        List<Alarm> open = alarmRepository.findAllByDeviceIdAndStatus(deviceId, Alarm.STATUS_OPEN);
        if (open.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        open.forEach(a -> {
            a.setStatus(Alarm.STATUS_RESOLVED);
            a.setResolvedAt(now);
            a.setResolvedBy("system");
        });
        alarmRepository.saveAll(open);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
