package com.iot.envmonitor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_data", indexes = {
        @Index(name = "idx_sensor_data_device_ts", columnList = "device_id,ts")
})
@Getter
@Setter
@NoArgsConstructor
public class SensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(nullable = false)
    private double temperature;

    @Column(nullable = false)
    private double humidity;

    /** 信号强度 dBm，可为空 */
    private Integer rssi;

    /** 电量百分比，可为空 */
    private Integer battery;

    @Column(nullable = false)
    private LocalDateTime ts;
}
