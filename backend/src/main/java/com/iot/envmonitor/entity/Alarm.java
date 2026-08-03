package com.iot.envmonitor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "alarms", indexes = {
        @Index(name = "idx_alarms_device_status", columnList = "device_id,status")
})
@Getter
@Setter
@NoArgsConstructor
public class Alarm {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_RESOLVED = "RESOLVED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    /** 报警类型：TEMPERATURE_HIGH / TEMPERATURE_LOW / HUMIDITY_HIGH / HUMIDITY_LOW */
    @Column(nullable = false, length = 32)
    private String type;

    /** 触发时的实测值 */
    @Column(name = "measured_value", nullable = false)
    private double value;

    /** 触发阈值 */
    @Column(nullable = false)
    private double threshold;

    @Column(length = 255)
    private String message;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by", length = 50)
    private String resolvedBy;
}
