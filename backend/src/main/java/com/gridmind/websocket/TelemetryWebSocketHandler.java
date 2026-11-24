package com.gridmind.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridmind.dto.TelemetryBroadcastDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class TelemetryWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TelemetryWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    public TelemetryWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket client connected: id={}, remoteAddress={}. Total active: {}",
                session.getId(), session.getRemoteAddress(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket client disconnected: id={}, status={}. Total active: {}",
                session.getId(), status, sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if ("ping".equalsIgnoreCase(payload.trim())) {
            session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
    }

    public void broadcastTelemetry(TelemetryBroadcastDto telemetry) {
        if (sessions.isEmpty()) {
            return;
        }

        try {
            String jsonPayload = objectMapper.writeValueAsString(telemetry);
            TextMessage message = new TextMessage(jsonPayload);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        log.debug("Failed sending telemetry to session {}: {}", session.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed serializing telemetry broadcast: {}", e.getMessage());
        }
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }
}
