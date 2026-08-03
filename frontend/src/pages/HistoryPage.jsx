import { useEffect, useMemo, useState } from 'react'
import { Card, DatePicker, Select, Space, Spin, message } from 'antd'
import ReactECharts from 'echarts-for-react'
import dayjs from 'dayjs'
import client from '../api/client'

export default function HistoryPage() {
  const [devices, setDevices] = useState([])
  const [deviceId, setDeviceId] = useState(null)
  const [range, setRange] = useState([dayjs().subtract(24, 'hour'), dayjs()])
  const [interval, setInterval] = useState(0)
  const [points, setPoints] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    client.get('/devices').then((list) => {
      setDevices(list)
      if (list.length > 0) setDeviceId(list[0].id)
    })
  }, [])

  useEffect(() => {
    if (!deviceId || !range || range.length !== 2) return
    const fetchData = async () => {
      setLoading(true)
      try {
        const params = {
          start: range[0].format('YYYY-MM-DDTHH:mm:ss'),
          end: range[1].format('YYYY-MM-DDTHH:mm:ss'),
          interval,
        }
        setPoints(await client.get(`/devices/${deviceId}/data`, { params }))
      } catch (error) {
        message.error(error.response?.data?.message || '历史数据加载失败')
      } finally {
        setLoading(false)
      }
    }
    fetchData()
  }, [deviceId, range, interval])

  const option = useMemo(
    () => ({
      tooltip: { trigger: 'axis' },
      legend: { data: ['温度 (°C)', '湿度 (%)'] },
      grid: { left: 60, right: 60, top: 50, bottom: 40 },
      xAxis: {
        type: 'time',
        axisLabel: { formatter: '{MM}-{dd} {HH}:{mm}' },
      },
      yAxis: [
        { type: 'value', name: '温度 (°C)', scale: true },
        { type: 'value', name: '湿度 (%)', scale: true, splitLine: { show: false } },
      ],
      series: [
        {
          name: '温度 (°C)',
          type: 'line',
          showSymbol: false,
          smooth: true,
          data: points.map((p) => [p.ts, p.temperature]),
          itemStyle: { color: '#fa541c' },
        },
        {
          name: '湿度 (%)',
          type: 'line',
          yAxisIndex: 1,
          showSymbol: false,
          smooth: true,
          data: points.map((p) => [p.ts, p.humidity]),
          itemStyle: { color: '#1677ff' },
        },
      ],
    }),
    [points],
  )

  return (
    <Card
      title="历史数据曲线"
      extra={
        <Space wrap>
          <Select
            value={deviceId}
            onChange={setDeviceId}
            style={{ width: 220 }}
            placeholder="选择设备"
            options={devices.map((d) => ({ value: d.id, label: `${d.name} (${d.id})` }))}
          />
          <DatePicker.RangePicker
            value={range}
            showTime
            onChange={setRange}
            presets={[
              { label: '最近1小时', value: [dayjs().subtract(1, 'hour'), dayjs()] },
              { label: '最近6小时', value: [dayjs().subtract(6, 'hour'), dayjs()] },
              { label: '最近24小时', value: [dayjs().subtract(24, 'hour'), dayjs()] },
              { label: '最近7天', value: [dayjs().subtract(7, 'day'), dayjs()] },
            ]}
          />
          <Select
            value={interval}
            onChange={setInterval}
            style={{ width: 140 }}
            options={[
              { value: 0, label: '自动聚合' },
              { value: 60, label: '1 分钟粒度' },
              { value: 300, label: '5 分钟粒度' },
              { value: 900, label: '15 分钟粒度' },
              { value: 3600, label: '1 小时粒度' },
            ]}
          />
        </Space>
      }
    >
      <Spin spinning={loading}>
        <ReactECharts option={option} style={{ height: 480 }} notMerge />
      </Spin>
    </Card>
  )
}
