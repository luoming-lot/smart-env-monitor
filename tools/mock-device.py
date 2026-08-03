#!/usr/bin/env python3
"""模拟 ESP32 设备，向 MQTT Broker 发布温湿度遥测数据。

用途：没有真实硬件时，验证平台完整链路（采集 → MQTT → 后端 → WebSocket → 前端）。
默认会周期性产生温度尖峰，便于观察报警的触发与自动恢复。

用法：
    pip install paho-mqtt
    python mock-device.py --devices 3 --interval 5
"""

import argparse
import json
import math
import random
import time

import paho.mqtt.client as mqtt


def build_device(index: int) -> dict:
    base_temp = 20 + (index % 3) * 4          # 各设备基准温度不同
    base_hum = 45 + (index % 2) * 15
    return {
        "id": f"esp32-{index + 1:03d}",
        "base_temp": base_temp,
        "base_hum": base_hum,
    }


def sample(device: dict, t: float, alarm_rate: float) -> dict:
    temperature = device["base_temp"] + 3 * math.sin(t / 60) + random.uniform(-0.4, 0.4)
    humidity = device["base_hum"] + 8 * math.sin(t / 90 + int(device["id"][-1])) + random.uniform(-1.5, 1.5)
    # 偶发高温尖峰，用于演示报警
    if random.random() < alarm_rate:
        temperature += 12
    return {
        "temperature": round(temperature, 1),
        "humidity": round(max(0, min(100, humidity)), 1),
        "rssi": random.randint(-72, -38),
        "battery": random.randint(60, 100),
        "firmwareVersion": "1.0.0",
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="模拟 ESP32 设备遥测上报")
    parser.add_argument("--broker", default="localhost", help="MQTT Broker 地址")
    parser.add_argument("--port", type=int, default=1883)
    parser.add_argument("--devices", type=int, default=3, help="模拟设备数量")
    parser.add_argument("--interval", type=float, default=5.0, help="上报间隔（秒）")
    parser.add_argument("--count", type=int, default=-1, help="总上报次数，-1 为无限")
    parser.add_argument("--alarm-rate", type=float, default=0.03, help="每次上报触发高温尖峰的概率")
    args = parser.parse_args()

    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    client.connect(args.broker, args.port, keepalive=60)
    client.loop_start()

    devices = [build_device(i) for i in range(args.devices)]
    print(f"开始模拟 {args.devices} 台设备 -> {args.broker}:{args.port}，"
          f"间隔 {args.interval}s（Ctrl+C 停止）")

    start = time.time()
    sent = 0
    try:
        while args.count < 0 or sent < args.count:
            for device in devices:
                topic = f"env/{device['id']}/telemetry"
                payload = json.dumps(sample(device, time.time() - start, args.alarm_rate))
                client.publish(topic, payload, qos=0)
                sent += 1
            time.sleep(args.interval)
    except KeyboardInterrupt:
        print(f"\n已停止，共发送 {sent} 条消息")
    finally:
        client.loop_stop()
        client.disconnect()


if __name__ == "__main__":
    main()
