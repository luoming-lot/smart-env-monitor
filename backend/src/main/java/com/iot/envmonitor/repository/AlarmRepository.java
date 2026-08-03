package com.iot.envmonitor.repository;

import com.iot.envmonitor.entity.Alarm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    List<Alarm> findAllByDeviceIdAndStatus(String deviceId, String status);

    Optional<Alarm> findFirstByDeviceIdAndTypeAndStatus(String deviceId, String type, String status);

    long countByStatus(String status);

    @Query("select a from Alarm a where (:status is null or a.status = :status) " +
            "and (:deviceId is null or a.deviceId = :deviceId)")
    Page<Alarm> search(@Param("status") String status, @Param("deviceId") String deviceId, Pageable pageable);
}
