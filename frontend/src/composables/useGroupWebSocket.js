import { ref, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export function useGroupWebSocket(groupId, onEvent) {
  const connected = ref(false)
  let client = null

  function connect() {
    client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        connected.value = true
        client.subscribe(`/topic/group/${groupId}`, (message) => {
          try {
            const event = JSON.parse(message.body)
            onEvent(event)
          } catch {
            // ignore malformed messages
          }
        })
      },
      onDisconnect: () => {
        connected.value = false
      },
      onStompError: () => {
        connected.value = false
      }
    })
    client.activate()
  }

  function disconnect() {
    if (client) {
      client.deactivate()
      client = null
      connected.value = false
    }
  }

  onUnmounted(disconnect)

  return { connected, connect, disconnect }
}
