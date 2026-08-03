import { Client } from '@stomp/stompjs'
import { createContext, useContext, useEffect, useState } from 'react'

const TelemetryContext = createContext({ readings: {} })

/**
 * 通过 STOMP/WebSocket 订阅实时遥测，按 deviceId 维护最新读数 Map。
 */
export function TelemetryProvider({ children }) {
  const [readings, setReadings] = useState({})

  useEffect(() => {
    const wsProtocol = location.protocol === 'https:' ? 'wss' : 'ws'
    const client = new Client({
      brokerURL: `${wsProtocol}://${location.host}/ws`,
      reconnectDelay: 3000,
      onConnect: () => {
        client.subscribe('/topic/telemetry', (message) => {
          const data = JSON.parse(message.body)
          setReadings((prev) => ({ ...prev, [data.deviceId]: data }))
        })
      },
    })
    client.activate()
    return () => client.deactivate()
  }, [])

  return <TelemetryContext.Provider value={{ readings }}>{children}</TelemetryContext.Provider>
}

export const useTelemetry = () => useContext(TelemetryContext)
