package com.iot.envmonitor.logic;

import com.iot.envmonitor.entity.Device;
import com.iot.envmonitor.logic.AlarmEvaluator.AlarmEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlarmEvaluatorTest {

    private Device device() {
        Device d = new Device();
        d.setId("esp32-test");
        d.setName("test");
        d.setTemperatureMin(-10);
        d.setTemperatureMax(35);
        d.setHumidityMin(20);
        d.setHumidityMax(80);
        return d;
    }

    @Test
    void highTemperatureTriggersAlarm() {
        List<AlarmEvent> events = AlarmEvaluator.evaluate(device(), 36.5, 50);
        assertEquals(1, events.size());
        assertEquals(AlarmEvaluator.TYPE_TEMP_HIGH, events.get(0).type());
        assertEquals(35.0, events.get(0).threshold());
        assertEquals(36.5, events.get(0).value());
    }

    @Test
    void lowHumidityTriggersAlarm() {
        List<AlarmEvent> events = AlarmEvaluator.evaluate(device(), 25, 15.5);
        assertEquals(1, events.size());
        assertEquals(AlarmEvaluator.TYPE_HUM_LOW, events.get(0).type());
    }

    @Test
    void boundaryValuesAreSafe() {
        assertTrue(AlarmEvaluator.evaluate(device(), 35.0, 80.0).isEmpty());
        assertTrue(AlarmEvaluator.evaluate(device(), -10.0, 20.0).isEmpty());
    }

    @Test
    void bothOutOfRangeProduceTwoEvents() {
        List<AlarmEvent> events = AlarmEvaluator.evaluate(device(), 40, 90);
        assertEquals(2, events.size());
    }
}
