CREATE TABLE users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(100) NOT NULL,
    nickname   VARCHAR(50),
    role       VARCHAR(20)  NOT NULL DEFAULT 'ADMIN',
    created_at DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_users_username (username)
);

CREATE TABLE devices (
    id               VARCHAR(64)  NOT NULL,
    name             VARCHAR(64)  NOT NULL,
    location         VARCHAR(128),
    firmware_version VARCHAR(32),
    temperature_min  DOUBLE       NOT NULL DEFAULT -20,
    temperature_max  DOUBLE       NOT NULL DEFAULT 45,
    humidity_min     DOUBLE       NOT NULL DEFAULT 20,
    humidity_max     DOUBLE       NOT NULL DEFAULT 80,
    last_seen        DATETIME(3),
    created_at       DATETIME(3)  NOT NULL,
    updated_at       DATETIME(3)  NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE sensor_data (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    device_id   VARCHAR(64) NOT NULL,
    temperature DOUBLE      NOT NULL,
    humidity    DOUBLE      NOT NULL,
    rssi        INT,
    battery     INT,
    ts          DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_sensor_data_device_ts (device_id, ts)
);

CREATE TABLE alarms (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    device_id    VARCHAR(64)  NOT NULL,
    type         VARCHAR(32)  NOT NULL,
    measured_value DOUBLE     NOT NULL,
    threshold    DOUBLE       NOT NULL,
    message      VARCHAR(255),
    status       VARCHAR(16)  NOT NULL,
    triggered_at DATETIME(3)  NOT NULL,
    resolved_at  DATETIME(3),
    resolved_by  VARCHAR(50),
    PRIMARY KEY (id),
    INDEX idx_alarms_device_status (device_id, status)
);
