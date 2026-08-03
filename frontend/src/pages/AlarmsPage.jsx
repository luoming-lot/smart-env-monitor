import { useCallback, useEffect, useState } from 'react'
import { Button, Card, Select, Space, Table, Tag, message } from 'antd'
import dayjs from 'dayjs'
import client from '../api/client'

const typeMeta = {
  TEMPERATURE_HIGH: { text: '温度过高', color: 'red' },
  TEMPERATURE_LOW: { text: '温度过低', color: 'blue' },
  HUMIDITY_HIGH: { text: '湿度过高', color: 'orange' },
  HUMIDITY_LOW: { text: '湿度过低', color: 'cyan' },
}

export default function AlarmsPage() {
  const [alarms, setAlarms] = useState({ content: [], totalElements: 0 })
  const [devices, setDevices] = useState([])
  const [status, setStatus] = useState('OPEN')
  const [deviceId, setDeviceId] = useState(null)
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const params = { page, size }
      if (status) params.status = status
      if (deviceId) params.deviceId = deviceId
      setAlarms(await client.get('/alarms', { params }))
    } finally {
      setLoading(false)
    }
  }, [status, deviceId, page, size])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    client.get('/devices').then(setDevices)
  }, [])

  const resolve = async (id) => {
    await client.put(`/alarms/${id}/resolve`)
    message.success('报警已处理')
    load()
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '设备 ID', dataIndex: 'deviceId' },
    {
      title: '报警类型',
      dataIndex: 'type',
      render: (type) => {
        const meta = typeMeta[type] || { text: type, color: 'default' }
        return <Tag color={meta.color}>{meta.text}</Tag>
      },
    },
    { title: '报警详情', dataIndex: 'message' },
    {
      title: '实测值 / 阈值',
      render: (_, r) => `${r.value.toFixed(1)} / ${r.threshold.toFixed(1)}`,
    },
    {
      title: '触发时间',
      dataIndex: 'triggeredAt',
      render: (v) => dayjs(v).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      render: (s) => (s === 'OPEN' ? <Tag color="error">未处理</Tag> : <Tag color="success">已处理</Tag>),
    },
    {
      title: '处理信息',
      render: (_, r) =>
        r.status === 'RESOLVED' ? `${r.resolvedBy || '-'} · ${dayjs(r.resolvedAt).format('MM-DD HH:mm')}` : '-',
    },
    {
      title: '操作',
      render: (_, r) =>
        r.status === 'OPEN' ? (
          <Button type="link" size="small" onClick={() => resolve(r.id)}>
            标记已处理
          </Button>
        ) : null,
    },
  ]

  return (
    <Card
      title="报警管理"
      extra={
        <Space>
          <Select
            value={status}
            onChange={(v) => { setStatus(v); setPage(0) }}
            style={{ width: 130 }}
            options={[
              { value: 'OPEN', label: '未处理' },
              { value: 'RESOLVED', label: '已处理' },
              { value: '', label: '全部' },
            ]}
          />
          <Select
            value={deviceId}
            onChange={(v) => { setDeviceId(v); setPage(0) }}
            allowClear
            placeholder="全部设备"
            style={{ width: 200 }}
            options={devices.map((d) => ({ value: d.id, label: d.name }))}
          />
        </Space>
      }
    >
      <Table
        dataSource={alarms.content}
        columns={columns}
        loading={loading}
        rowKey="id"
        pagination={{
          current: page + 1,
          pageSize: size,
          total: alarms.totalElements,
          showSizeChanger: true,
          onChange: (p, s) => { setPage(p - 1); setSize(s) },
        }}
      />
    </Card>
  )
}
