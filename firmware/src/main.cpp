/**
 * 智能环境监测平台 - ESP32 温湿度采集固件
 *
 * 功能：读取 DHT22 温湿度，通过 MQTT 定期上报 JSON 遥测数据，
 * 支持 WiFi/MQTT 断线自动重连、信号强度与固件版本上报。
 *
 * 消息格式（发布到 env/<deviceId>/telemetry）：
 * {
 *   "temperature": 25.3,
 *   "humidity": 58.6,
 *   "rssi": -52,
 *   "battery": null,
 *   "firmwareVersion": "1.0.0"
 * }
 */
#include <Arduino.h>
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <DHT.h>
#include "config.h"

// ---------------------------------------------------------------- 配置
#ifndef TEMP_OFFSET
#define TEMP_OFFSET 0.0f
#endif
#ifndef HUM_OFFSET
#define HUM_OFFSET 0.0f
#endif

const char* FIRMWARE_VERSION = "1.0.0";

// ---------------------------------------------------------------- 全局
DHT dht(DHT_PIN, DHT_TYPE);
WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);
unsigned long lastPublishAt = 0;
unsigned long lastReconnectAttemptAt = 0;
String deviceId;

// ---------------------------------------------------------------- 工具
String makeDeviceId() {
  // 取 ESP32 出厂 MAC 后 6 位，保证设备 ID 全局唯一且无需手工配置
  uint64_t mac = ESP.getEfuseMac();
  char buf[16];
  snprintf(buf, sizeof(buf), "esp32-%06llX", (mac & 0xFFFFFF));
  return String(buf);
}

void connectWifi() {
  if (WiFi.status() == WL_CONNECTED) {
    return;
  }
  Serial.printf("[WiFi] 连接 %s ...\n", WIFI_SSID);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  int tries = 0;
  while (WiFi.status() != WL_CONNECTED && tries < 40) {
    delay(500);
    tries++;
  }
  if (WiFi.status() == WL_CONNECTED) {
    Serial.printf("[WiFi] 已连接，IP: %s\n", WiFi.localIP().toString().c_str());
  } else {
    Serial.println("[WiFi] 连接失败，稍后重试");
  }
}

bool connectMqtt() {
  if (mqttClient.connected()) {
    return true;
  }
  Serial.printf("[MQTT] 连接 %s:%d ...\n", MQTT_BROKER, MQTT_PORT);
  mqttClient.setServer(MQTT_BROKER, MQTT_PORT);
  boolean ok = false;
  if (strlen(MQTT_USERNAME) > 0) {
    ok = mqttClient.connect(deviceId.c_str(), MQTT_USERNAME, MQTT_PASSWORD);
  } else {
    ok = mqttClient.connect(deviceId.c_str());
  }
  if (ok) {
    Serial.println("[MQTT] 已连接");
  }
  return ok;
}

void publishTelemetry() {
  float temperature = dht.readTemperature();
  float humidity = dht.readHumidity();
  if (isnan(temperature) || isnan(humidity)) {
    Serial.println("[Sensor] 读取 DHT 失败，跳过本次上报");
    return;
  }

  temperature += TEMP_OFFSET;
  humidity += HUM_OFFSET;
  humidity = constrain(humidity, 0.0f, 100.0f);

  JsonDocument doc;
  doc["temperature"] = roundf(temperature * 10) / 10;
  doc["humidity"] = roundf(humidity * 10) / 10;
  doc["rssi"] = WiFi.RSSI();
  doc["battery"] = nullptr;   // 无电池供电时上报 null
  doc["firmwareVersion"] = FIRMWARE_VERSION;

  String payload;
  serializeJson(doc, payload);
  String topic = String(MQTT_TOPIC_PREFIX) + deviceId + "/telemetry";
  if (mqttClient.publish(topic.c_str(), payload.c_str(), false)) {
    digitalWrite(STATUS_LED_PIN, !digitalRead(STATUS_LED_PIN));
    Serial.printf("[MQTT] 发布 %s: %s\n", topic.c_str(), payload.c_str());
  } else {
    Serial.println("[MQTT] 发布失败");
  }
}

// ---------------------------------------------------------------- 主流程
void setup() {
  Serial.begin(115200);
  pinMode(STATUS_LED_PIN, OUTPUT);
  dht.begin();
  deviceId = makeDeviceId();
  Serial.printf("[Boot] 设备 ID: %s，固件版本: %s\n", deviceId.c_str(), FIRMWARE_VERSION);
  connectWifi();
  connectMqtt();
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    connectWifi();
  }
  if (!mqttClient.connected()) {
    unsigned long now = millis();
    if (now - lastReconnectAttemptAt > 5000) {
      lastReconnectAttemptAt = now;
      connectMqtt();
    }
  } else {
    mqttClient.loop();
  }

  unsigned long now = millis();
  if (now - lastPublishAt >= PUBLISH_INTERVAL_MS) {
    lastPublishAt = now;
    publishTelemetry();
  }
}
