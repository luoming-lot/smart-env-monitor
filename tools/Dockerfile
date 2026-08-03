FROM python:3.12-slim
WORKDIR /app
COPY tools/mock-device.py /app/tools/mock-device.py
RUN pip install --no-cache-dir paho-mqtt
CMD ["python", "/app/tools/mock-device.py", "--broker", "mosquitto", "--interval", "5", "--alarm-rate", "0.05"]
