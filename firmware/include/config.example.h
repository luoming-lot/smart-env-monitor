#ifndef CONFIG_H
#define CONFIG_H

/* ==================== 使用说明 ====================
 * 1. 复制本文件为 config.h 并填写以下配置；
 * 2. PlatformIO 会在 include/ 下自动找到 config.h；
 * 3. 烧录前确认 WiFi 与 MQTT Broker 可达。
 * ================================================== */

// WiFi
#define WIFI_SSID       "your-wifi-ssid"
#define WIFI_PASSWORD   "your-wifi-password"

// MQTT Broker（mosquitto / EMQX 等）
#define MQTT_BROKER     "192.168.1.100"
#define MQTT_PORT       1883
#define MQTT_USERNAME   ""
#define MQTT_PASSWORD   ""

// 上报主题：env/<设备ID>/telemetry，后台通过该主题自动注册设备
#define MQTT_TOPIC_PREFIX "env/"

// 传感器与采样
#define DHT_PIN         4       // GPIO 连接 DHT22 数据脚
#define DHT_TYPE        DHT22   // DHT11 或 DHT22
#define PUBLISH_INTERVAL_MS  10000  // 上报周期（毫秒）

// 状态指示灯（板载 LED），发布成功时翻转
#define STATUS_LED_PIN  2

/* 校准偏移量：真实硬件上传感器可能存在系统性偏差，
 * 直接改这里即可，无需重新烧录程序逻辑。 */
#define TEMP_OFFSET     0.0f    // 温度校准偏移（摄氏度）
#define HUM_OFFSET      0.0f    // 湿度校准偏移（相对湿度 %）

#endif
