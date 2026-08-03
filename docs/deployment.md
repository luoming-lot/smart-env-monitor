# 部署说明

## 1. Docker Compose 部署（单机）

### 前置条件

- Docker ≥ 20.10、Docker Compose ≥ 2.0
- 可用的 80 / 1883 / 3306 / 8080 端口

### 部署步骤

```bash
# 1. 克隆项目
git clone https://github.com/luoming-lot/temp-humidity-monitor.git
cd temp-humidity-monitor

# 2.（可选）设置 JWT 密钥
cp .env.example .env
# 编辑 .env，将 JWT_SECRET 改为足够长的随机字符串

# 3. 构建并启动全部服务
docker compose up -d --build

# 4. 查看状态与日志
docker compose ps
docker compose logs -f backend
```

### 服务清单

| 容器 | 镜像 | 端口 | 说明 |
| --- | --- | --- | --- |
| mosquitto | eclipse-mosquitto:2 | 1883 | MQTT Broker，匿名接入（开发配置） |
| mysql | mysql:8.0 | 3306 | 主数据库，Flyway 自动建表 |
| backend | 自构建 | 8080 | Spring Boot API + MQTT 订阅 + WebSocket |
| frontend | 自构建 | 80 | Nginx 托管前端并反向代理 API/WebSocket |

### 验证

```bash
# 健康检查：登录接口
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 产生模拟数据
python tools/mock-device.py --devices 3
```

浏览器访问 http://localhost 即可看到实时数据。

## 2. 生产环境建议

- **密钥管理**：通过 `JWT_SECRET`、`MYSQL_PASSWORD` 等环境变量注入，不写入代码库；
- **MQTT 安全**：为 Mosquitto 配置 `password_file` 并关闭匿名访问（参考官方文档），后端通过 `MQTT_USERNAME` / `MQTT_PASSWORD` 接入；
- **数据库**：开启 MySQL 定期备份；多节点部署时可将 `sensor_data` 迁移至时序数据库；
- **反向代理**：生产环境建议在 Nginx 前置 HTTPS 证书，并限制 CORS 白名单；
- **监控告警**：为后端增加 Spring Boot Actuator 健康检查，接入 Prometheus 监控。

## 3. 本地开发（不使用 Docker）

### 依赖

- JDK 17+、Maven 3.8+
- Node.js 18+、npm
- MySQL 8.0（或使用 H2 内存库）

### 步骤

```bash
# 1. 创建数据库
mysql -uroot -p -e "CREATE DATABASE env_monitor CHARACTER SET utf8mb4;"

# 2. 后端（默认连 localhost:3306，账号 iot/iot123456，可通过环境变量覆盖）
cd backend
mvn spring-boot:run

# 3. 前端（Vite 已配置 /api、/ws 代理到 8080）
cd ../frontend
npm install
npm run dev
```

使用 H2 内存数据库时无需 MySQL：

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 4. 配置项

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `MYSQL_URL` | `jdbc:mysql://localhost:3306/env_monitor?...` | JDBC 连接串 |
| `MYSQL_USER` / `MYSQL_PASSWORD` | `iot` / `iot123456` | 数据库账号 |
| `MQTT_BROKER_URL` | `tcp://localhost:1883` | MQTT Broker 地址 |
| `MQTT_CLIENT_ID` | `env-monitor-backend` | MQTT 客户端 ID |
| `MQTT_TOPIC_FILTER` | `env/+/telemetry` | 订阅主题 |
| `JWT_SECRET` | 内置开发值 | JWT 签名密钥（≥32 字节） |
| `JWT_EXPIRATION_MS` | `86400000` | Token 有效期（毫秒） |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost` | 允许的前端来源 |
