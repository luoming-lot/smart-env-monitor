package com.iot.envmonitor.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.envmonitor.dto.TelemetryDtos.TelemetryReading;
import com.iot.envmonitor.service.TelemetryService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MQTT 连接管理：启动时连接 broker 并订阅设备遥测主题，
 * 断线后每 5 秒自动重连。主题格式：env/{deviceId}/telemetry
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttService implements MqttCallback {

    private final TelemetryService telemetryService;
    private final ObjectMapper objectMapper;

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    @Value("${mqtt.topic-filter}")
    private String topicFilter;

    private MqttClient client;
    private final ScheduledExecutorService reconnector = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean reconnecting = false;

    @PostConstruct
    public void init() {
        try {
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            client.setCallback(this);
            connect();
        } catch (MqttException e) {
            log.error("MQTT 客户端初始化失败，将自动重试", e);
            scheduleReconnect();
        }
    }

    private synchronized void connect() {
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setKeepAliveInterval(30);
            options.setConnectionTimeout(10);
            if (username != null && !username.isBlank()) {
                options.setUserName(username);
                options.setPassword(password == null ? null : password.toCharArray());
            }
            client.connect(options);
            client.subscribe(topicFilter, 0);
            log.info("MQTT 已连接 {}，订阅 {}", brokerUrl, topicFilter);
        } catch (MqttException e) {
            log.warn("MQTT 连接失败: {}", e.getMessage());
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (reconnecting) {
            return;
        }
        reconnecting = true;
        reconnector.schedule(() -> {
            reconnecting = false;
            if (!client.isConnected()) {
                connect();
            }
        }, 5, TimeUnit.SECONDS);
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT 连接断开: {}", cause == null ? "unknown" : cause.getMessage());
        scheduleReconnect();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            String deviceId = topic.split("/")[1];
            TelemetryReading reading = objectMapper.readValue(
                    new String(message.getPayload(), StandardCharsets.UTF_8), TelemetryReading.class);
            telemetryService.ingest(deviceId, reading);
        } catch (Exception e) {
            log.warn("处理 MQTT 消息失败 topic={}: {}", topic, e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // 仅订阅不收发业务消息
    }

    @PreDestroy
    public void destroy() {
        reconnector.shutdownNow();
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
            if (client != null) {
                client.close();
            }
        } catch (MqttException e) {
            log.warn("MQTT 关闭异常: {}", e.getMessage());
        }
    }
}
