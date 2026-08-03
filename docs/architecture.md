# 系统架构设计

## 1. 总体架构

```mermaid
flowchart LR
    subgraph 感知层
        ESP32["ESP32 + DHT22 传感器"]
        SIM["模拟设备<br/>(tools/mock-device.py)"]
    end

    subgraph 传输层
        MQTT["MQTT Broker<br/>(Eclipse Mosquitto)"]
    end

    subgraph 服务层 [Spring Boot 后端]
        INGEST["MQTT 接入服务<br/>遥测采集/设备登记"]
        ALARM["报警引擎<br/>阈值判定/自动闭环"]
        WS["WebSocket 推送<br/>(STOMP)"]
        API["REST API<br/>(JWT 鉴权)"]
    end

    subgraph 数据层
        DB[("MySQL<br/>设备/遥测/报警/用户")]
    end

    subgraph 展示层
        UI["React 前端<br/>(Ant Design + ECharts)"]
    end

    ESP32 -- "MQTT env/&lt;设备ID&gt;/telemetry" --> MQTT
    SIM -- "MQTT（模拟数据）" --> MQTT
    MQTT -- "订阅遥测主题" --> INGEST
    INGEST --> DB
    INGEST --> ALARM
    ALARM --> DB
    INGEST -- "实时读数" --> WS
    UI -- "REST API (JWT)" --> API
    API --> DB
    API --> ALARM
    WS -- "WebSocket 实时推送" --> UI
```

## 2. 数据流（一次完整的遥测上报）

```mermaid
sequenceDiagram
    participant D as ESP32 / 模拟设备
    participant B as MQTT Broker
    participant S as Spring Boot 后端
    participant DB as MySQL
    participant F as React 前端

    D->>B: publish env/{deviceId}/telemetry {温度, 湿度, ...}
    B->>S: 订阅消息（MQTT 回调）
    S->>S: 校验数据合法性
    S->>DB: 设备不存在则自动注册
    S->>DB: 写入 sensor_data 遥测记录
    S->>S: 报警引擎阈值判定
    alt 超出阈值
        S->>DB: 写入 OPEN 报警记录
    else 恢复区间
        S->>DB: 自动关闭未处理报警
    end
    S-->>F: WebSocket 推送 /topic/telemetry（实时刷新）
    F->>S: REST 查询历史曲线 /api/devices/{id}/data
    S-->>F: 聚合后的时间序列数据
    F->>F: ECharts 渲染曲线
```

## 3. 关键设计

### 3.1 设备自动注册与在线判定

- 设备首次上报时，后端按 `deviceId` 自动创建设备记录，无需手工录入；
- 设备表维护 `last_seen` 字段，超过 120 秒未上报即判定为离线（前端按时间实时计算）；
- 前端通过 WebSocket 实时订阅最新读数，离线/在线状态无需刷新页面即可更新。

### 3.2 报警引擎

- 每台设备独立配置温度、湿度上下限阈值；
- 采用「事件触发 + 状态闭环」模型：超阈值生成 `OPEN` 报警，恢复区间后自动闭环为 `RESOLVED`，也支持人工标记处理；
- 同一类型报警未处理期间不会重复产生（幂等），避免报警风暴。

### 3.3 历史数据聚合

- 历史曲线接口支持按时间窗口聚合（自动或手动指定粒度），将海量原始点降采样为可读曲线；
- 当前在应用层聚合，适合中小规模部署；规模化后可替换为数据库侧聚合或时序数据库。

### 3.4 安全设计

- 登录使用 BCrypt 加盐哈希存储密码；
- 接口采用 JWT（HS256）无状态鉴权，Spring Security 过滤器统一校验；
- 支持 CORS 白名单配置，前后端分离部署时按需放行来源。

## 4. 部署拓扑

```mermaid
flowchart LR
    U["浏览器"] --> N["Nginx<br/>静态资源 + 反向代理"]
    N --> F["React SPA<br/>(容器内)"]
    N -->|"/api, /ws"| BE["Spring Boot<br/>:8080"]
    BE --> MY["MySQL :3306"]
    BE --> MO["Mosquitto :1883"]
    E["ESP32 设备"] --> MO
```

单机 `docker compose up` 即可完成全栈部署；也可将各组件独立部署到不同主机。
