package com.iot.envmonitor.logic;

import com.iot.envmonitor.entity.Device;

import java.util.ArrayList;
import java.util.List;

/**
 * 纯函数式报警判定，便于单元测试。
 * 温度/湿度超出设备配置的阈值时产出报警事件；恢复区间内返回空列表。
 */
public final class AlarmEvaluator {

    public static final String TYPE_TEMP_HIGH = "TEMPERATURE_HIGH";
    public static final String TYPE_TEMP_LOW = "TEMPERATURE_LOW";
    public static final String TYPE_HUM_HIGH = "HUMIDITY_HIGH";
    public static final String TYPE_HUM_LOW = "HUMIDITY_LOW";

    private AlarmEvaluator() {
    }

    public record AlarmEvent(String type, double value, double threshold, String message) {
    }

    public static List<AlarmEvent> evaluate(Device device, double temperature, double humidity) {
        List<AlarmEvent> events = new ArrayList<>(2);
        if (temperature > device.getTemperatureMax()) {
            events.add(new AlarmEvent(TYPE_TEMP_HIGH, temperature, device.getTemperatureMax(),
                    String.format("温度过高：%.1f°C，超过阈值 %.1f°C", temperature, device.getTemperatureMax())));
        } else if (temperature < device.getTemperatureMin()) {
            events.add(new AlarmEvent(TYPE_TEMP_LOW, temperature, device.getTemperatureMin(),
                    String.format("温度过低：%.1f°C，低于阈值 %.1f°C", temperature, device.getTemperatureMin())));
        }
        if (humidity > device.getHumidityMax()) {
            events.add(new AlarmEvent(TYPE_HUM_HIGH, humidity, device.getHumidityMax(),
                    String.format("湿度过高：%.1f%%，超过阈值 %.1f%%", humidity, device.getHumidityMax())));
        } else if (humidity < device.getHumidityMin()) {
            events.add(new AlarmEvent(TYPE_HUM_LOW, humidity, device.getHumidityMin(),
                    String.format("湿度过低：%.1f%%，低于阈值 %.1f%%", humidity, device.getHumidityMin())));
        }
        return events;
    }
}
