import { useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { useStats } from '../context/StatsContext';

/**
 * Establishes a single WebSocket (SockJS + STOMP) connection and subscribes to /topic/stats.
 * Pushes each message into the global StatsContext.
 * Uses current origin so Vite dev proxy (proxy /ws to backend) works.
 */
export function useWebSocket() {
  const { updateStats, setConnectionState, setConnectionError } = useStats();
  const clientRef = useRef(null);

  useEffect(() => {
    const wsUrl = typeof window !== 'undefined'
      ? (import.meta.env.VITE_WS_URL || `${window.location.origin}/ws`)
      : '';

    if (!wsUrl) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 3000,
      connectionTimeout: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        setConnectionState(true);
        setConnectionError(null);
        client.subscribe('/topic/stats', (message) => {
          try {
            const body = JSON.parse(message.body);
            updateStats(body);
          } catch (e) {
            console.warn('Failed to parse stats message', e);
          }
        });
      },
      onStompError: (frame) => {
        setConnectionState(false);
        setConnectionError(frame.headers?.message || 'WebSocket error');
      },
      onWebSocketClose: () => {
        setConnectionState(false);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      setConnectionState(false);
    };
  }, [updateStats, setConnectionState, setConnectionError]);

  return { client: clientRef.current };
}
