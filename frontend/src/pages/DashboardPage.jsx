import { useEffect, useState } from 'react'
import { Alert, Card, Col, Row, Statistic, Table, Tag } from 'antd'
import dayjs from 'dayjs'
import client from '../api/client'
import { useTelemetry } from '../realtime/TelemetryContext.jsx'

const ONLINE_TIMEOUT_SECONDS = 120

export default function DashboardPage() {
  const [summary, setSummary] = useState(null)
  const { readings } = useTelemetry()

  const load = () => client.get('/dashboard/summary').then(setSummary)

  useEffect(() => {
    load()
    const timer = setInterval(load, 30000)
    return () => clearInterval(timer)
  }, [])

  const rows = (summary?.realtime || []).map((item) => {
    const live = readings[item.deviceId]
    const merged = live || item
    const online = dayjs(merged.ts).isAfter(dayjs().subtract(ONLINE_TIMEOUT_SECONDS, 'second'))
    return { ...merged, key: merged.deviceId, online }
  })

  const columns = [
    { title: '设备 ID', dataIndex: 'deviceId' },
    { title: '设备名称', dataIndex: 'deviceName' },
    {
      title: '温度 (°C)',
      dataIndex: 'temperature',
      render: (v) => (v == null ? '-' : <Tag color={v > 35 ? 'red' : v < 0 ? 'blue' : 'green'}>{v.toFixed(1)}</Tag>),
    },
    {
      title: '湿度 (%)',
      dataIndex: 'humidity',
      render: (v) => (v == null ? '-' : <Tag color={v > 80 || v < 20 ? 'orange' : 'cyan'}>{v.toFixed(1)}</Tag>),
    },
    { title: '信号 (dBm)', dataIndex: 'rssi', render: (v) => v ?? '-' },
    { title: '电量 (%)', dataIndex: 'battery', render: (v) => (v == null ? '-' : `${v}%`) },
    { title: '更新时间', dataIndex: 'ts', render: (v) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-') },
    {
      title: '状态',
      dataIndex: 'online',
      render: (online) => (online ? <Tag color="success">在线</Tag> : <Tag color="default">离线</Tag>),
    },
  ]

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Alert
        type="success"
        showIcon
        message="实时监控已开启"
        description="设备上报数据后，页面通过 WebSocket 实时刷新，无需手动刷新。"
      />
      <Row gutter={16}>
        <Col span={6}>
          <Card>
            <Statistic title="设备总数" value={summary?.deviceCount ?? '-'} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="在线设备" value={summary?.onlineCount ?? '-'} valueStyle={{ color: '#3f8600' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="未处理报警" value={summary?.openAlarmCount ?? '-'} valueStyle={{ color: '#cf1322' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="近1小时均值"
              value={
                summary?.avgTemperature != null && summary?.avgHumidity != null
                  ? `${summary.avgTemperature.toFixed(1)}°C / ${summary.avgHumidity.toFixed(1)}%`
                  : '-'
              }
            />
          </Card>
        </Col>
      </Row>
      <Card title="设备实时数据">
        <Table dataSource={rows} columns={columns} pagination={false} size="middle" />
      </Card>
    </div>
  )
}
