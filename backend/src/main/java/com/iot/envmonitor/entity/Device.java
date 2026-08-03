package com.iot.envmonitor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
public class Device {

    /** 设备 ID（MQTT 消息中的 deviceId，如 esp32-001） */
    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 128)
    private String location;

    @Column(name = "firmware_version", length = 32)
    private String firmwareVersion;

    /** 报警阈值（摄氏度） */
    @Column(name = "temperature_min", nullable = false)
    private double temperatureMin = -20;

    @Column(name = "temperature_max", nullable = false)
    private double temperatureMax = 45;

    /** 报警阈值（相对湿度 %） */
    @Column(name = "humidity_min", nullable = false)
    private double humidityMin = 20;

    @Column(name = "humidity_max", nullable = false)
    private double humidityMax = 80;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
