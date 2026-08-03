import { useCallback, useEffect, useState } from 'react'
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Space,
  Table,
  Tag,
  message,
} from 'antd'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import client from '../api/client'

const defaultThresholds = {
  temperatureMin: -20,
  temperatureMax: 45,
  humidityMin: 20,
  humidityMax: 80,
}

export default function DevicesPage() {
  const [devices, setDevices] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form] = Form.useForm()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setDevices(await client.get('/devices'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
    const timer = setInterval(load, 15000)
    return () => clearInterval(timer)
  }, [load])

  const openCreate = () => {
    setEditing(null)
    form.setFieldsValue({ ...defaultThresholds })
    setModalOpen(true)
  }

  const openEdit = (device) => {
    setEditing(device)
    form.setFieldsValue({
      id: device.id,
      name: device.name,
      location: device.location,
      temperatureMin: device.temperatureMin,
      temperatureMax: device.temperatureMax,
      humidityMin: device.humidityMin,
      humidityMax: device.humidityMax,
    })
    setModalOpen(true)
  }

  const submit = async () => {
    const values = await form.validateFields()
    try {
      if (editing) {
        await client.put(`/devices/${editing.id}`, values)
        message.success('设备已更新')
      } else {
        await client.post('/devices', values)
        message.success('设备已创建')
      }
      setModalOpen(false)
      load()
    } catch (error) {
      message.error(error.response?.data?.message || '保存失败')
    }
  }

  const remove = async (id) => {
    await client.delete(`/devices/${id}`)
    message.success('设备已删除')
    load()
  }

  const columns = [
    { title: '设备 ID', dataIndex: 'id' },
    { title: '名称', dataIndex: 'name' },
    { title: '位置', dataIndex: 'location', render: (v) => v || '-' },
    { title: '固件版本', dataIndex: 'firmwareVersion', render: (v) => v || '-' },
    {
      title: '温度阈值 (°C)',
      render: (_, r) => `${r.temperatureMin.toFixed(0)} ~ ${r.temperatureMax.toFixed(0)}`,
    },
    {
      title: '湿度阈值 (%)',
      render: (_, r) => `${r.humidityMin.toFixed(0)} ~ ${r.humidityMax.toFixed(0)}`,
    },
    {
      title: '最新读数',
      render: (_, r) =>
        r.latestTemperature != null ? `${r.latestTemperature.toFixed(1)}°C / ${r.latestHumidity.toFixed(1)}%` : '-',
    },
    { title: '最近上报', dataIndex: 'lastSeen', render: (v) => (v ? dayjs(v).format('MM-DD HH:mm:ss') : '-') },
    {
      title: '状态',
      dataIndex: 'online',
      render: (online) => (online ? <Tag color="success">在线</Tag> : <Tag color="default">离线</Tag>),
    },
    {
      title: '操作',
      render: (_, r) => (
        <Space>
          <Button type="link" size="small" onClick={() => openEdit(r)}>
            编辑
          </Button>
          <Popconfirm title="确定删除该设备？" description="历史数据会保留" onConfirm={() => remove(r.id)}>
            <Button type="link" size="small" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <Card
      title="设备管理"
      extra={
        <Space>
          <Button icon={<ReloadOutlined />} onClick={load}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增设备
          </Button>
        </Space>
      }
    >
      <Table dataSource={devices} columns={columns} loading={loading} rowKey="id" pagination={false} />

      <Modal
        title={editing ? '编辑设备' : '新增设备'}
        open={modalOpen}
        onOk={submit}
        onCancel={() => setModalOpen(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="id"
            label="设备 ID"
            rules={[{ required: true, message: '请输入设备 ID' }]}
            extra="与固件中上报的 deviceId 保持一致"
          >
            <Input disabled={!!editing} placeholder="如 esp32-001" />
          </Form.Item>
          <Form.Item name="name" label="设备名称" rules={[{ required: true, message: '请输入设备名称' }]}>
            <Input placeholder="如 实验室温湿度计" />
          </Form.Item>
          <Form.Item name="location" label="安装位置">
            <Input placeholder="如 3 号楼 201 实验室" />
          </Form.Item>
          <Space size="large" wrap>
            <Form.Item name="temperatureMin" label="温度下限 (°C)">
              <InputNumber />
            </Form.Item>
            <Form.Item name="temperatureMax" label="温度上限 (°C)">
              <InputNumber />
            </Form.Item>
            <Form.Item name="humidityMin" label="湿度下限 (%)">
              <InputNumber />
            </Form.Item>
            <Form.Item name="humidityMax" label="湿度上限 (%)">
              <InputNumber />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </Card>
  )
}
