package com.iot.envmonitor.repository;

import com.iot.envmonitor.entity.SensorData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    List<SensorData> findByDeviceIdAndTsBetweenOrderByTsAsc(String deviceId, LocalDateTime start, LocalDateTime end);

    /** 每个设备的最新一条数据 */
    @Query("select d from SensorData d where d.ts in (select max(s.ts) from SensorData s group by s.deviceId)")
    List<SensorData> findLatestPerDevice();

    /** 最近一段时间内的平均值，用于仪表盘汇总 */
    @Query("select avg(d.temperature), avg(d.humidity) from SensorData d where d.ts > :since")
    List<Object[]> findAverageSince(LocalDateTime since);
}
