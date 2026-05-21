import { useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export const useWebSocket = <T>(
  topic: string,
  onMessage: (data: T) => void,
  enabled = true
) => {
  const clientRef = useRef<Client | null>(null);
  const onMessageRef = useRef(onMessage);
  onMessageRef.current = onMessage;

  const connect = useCallback(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(topic, (msg) => {
          try {
            const data = JSON.parse(msg.body) as T;
            onMessageRef.current(data);
          } catch {
            // ignore parse errors
          }
        });
      },
      onStompError: () => {
        // WebSocket not available in this environment — silently skip
      },
    });
    client.activate();
    clientRef.current = client;
  }, [topic]);

  useEffect(() => {
    if (!enabled) return;
    connect();
    return () => {
      clientRef.current?.deactivate();
    };
  }, [enabled, connect]);
};
