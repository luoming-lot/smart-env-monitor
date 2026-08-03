package com.iot.envmonitor.repository;

import com.iot.envmonitor.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, String> {
}
