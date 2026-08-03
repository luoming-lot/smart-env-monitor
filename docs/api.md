# 接口文档

Base URL：`http://localhost:8080/api`（本机/ Docker 部署）

在线演示环境的 Base URL 与前端同源：`https://env-monitor-60z3.onrender.com/api`

所有接口（除登录外）均需在请求头携带 Token：

```
Authorization: Bearer <token>
```

统一错误响应格式：

```json
{
  "status": "error",
  "message": "错误说明",
  "timestamp": "2026-08-03T10:00:00"
}
```

## 1. 认证

### POST /auth/login — 登录

请求：

```json
{ "username": "admin", "password": "admin123" }
```

响应 `200`：

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": { "id": 1, "username": "admin", "nickname": "系统管理员", "role": "ADMIN" }
}
```

### GET /auth/me — 当前用户信息

响应 `200`：与登录响应中的 `user` 结构一致。

## 2. 设备管理

### GET /devices?keyword=实验室

查询参数：`keyword`（可选，按设备 ID / 名称模糊匹配）。

响应 `200`：

```json
[
  {
    "id": "esp32-001",
    "name": "实验室温湿度计",
    "location": "3 号楼 201",
    "firmwareVersion": "1.0.0",
    "temperatureMin": -20,
    "temperatureMax": 35,
    "humidityMin": 20,
    "humidityMax": 80,
    "online": true,
    "lastSeen": "2026-08-03T10:00:00",
    "latestTemperature": 26.4,
    "latestHumidity": 58.2,
    "createdAt": "2026-08-01T09:00:00"
  }
]
```

### POST /devices — 新增设备

请求：

```json
{
  "id": "esp32-001",
  "name": "实验室温湿度计",
  "location": "3 号楼 201",
  "temperatureMin": -20,
  "temperatureMax": 35,
  "humidityMin": 20,
  "humidityMax": 80
}
```

`id` 必填且唯一；设备首次通过 MQTT 上报时也会自动注册，无需手工创建。

### PUT /devices/{id} — 更新设备

请求体同 POST，未传的阈值字段保持原值。

### DELETE /devices/{id} — 删除设备

删除设备记录（历史遥测数据保留，便于审计）。

## 3. 历史数据

### GET /devices/{id}/data?start=...&end=...&interval=300

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `start` | 否 | 开始时间，ISO 格式 `YYYY-MM-DDTHH:mm:ss`，默认最近 24 小时 |
| `end` | 否 | 结束时间，默认当前时间 |
| `interval` | 否 | 聚合窗口（秒），`0` 表示按时间跨度自动选择 |

响应 `200`（每个元素为聚合窗口内的平均值）：

```json
[
  { "ts": "2026-08-03T09:55:00", "temperature": 26.1, "humidity": 57.8 },
  { "ts": "2026-08-03T10:00:00", "temperature": 26.4, "humidity": 58.2 }
]
```

## 4. 报警管理

### GET /alarms?status=OPEN&deviceId=esp32-001&page=0&size=10

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `status` | 否 | `OPEN` / `RESOLVED`，不传查全部 |
| `deviceId` | 否 | 按设备过滤 |
| `page` / `size` | 否 | 分页，默认 `0` / `10` |

响应 `200`：Spring Data 分页结构，`content` 中的报警字段：

```json
{
  "content": [
    {
      "id": 1,
      "deviceId": "esp32-001",
      "type": "TEMPERATURE_HIGH",
      "value": 36.5,
      "threshold": 35.0,
      "message": "温度过高：36.5°C，超过阈值 35.0°C",
      "status": "OPEN",
      "triggeredAt": "2026-08-03T10:05:00",
      "resolvedAt": null,
      "resolvedBy": null
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

报警类型：`TEMPERATURE_HIGH` / `TEMPERATURE_LOW` / `HUMIDITY_HIGH` / `HUMIDITY_LOW`。

### PUT /alarms/{id}/resolve — 标记已处理

响应 `200`：返回更新后的报警记录（`status=RESOLVED`，`resolvedBy` 为当前登录用户）。

## 5. 仪表盘

### GET /dashboard/summary — 汇总统计

```json
{
  "deviceCount": 3,
  "onlineCount": 2,
  "openAlarmCount": 1,
  "avgTemperature": 26.3,
  "avgHumidity": 57.9,
  "realtime": [
    {
      "deviceId": "esp32-001",
      "deviceName": "实验室温湿度计",
      "temperature": 26.4,
      "humidity": 58.2,
      "rssi": -52,
      "battery": 87,
      "ts": "2026-08-03T10:00:00"
    }
  ]
}
```

### GET /dashboard/realtime — 各设备最新读数

响应 `200`：与 `summary.realtime` 结构一致。

## 6. WebSocket 实时推送

- 端点：`/ws`（STOMP over WebSocket）
- 订阅主题：`/topic/telemetry`，每台设备上报后推送该设备最新读数（结构同 `realtime` 元素）；
- 浏览器连接示例见前端 `src/realtime/TelemetryContext.jsx`。
