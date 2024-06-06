package com.example.damka;

public interface WebSocketListener {
    void onWebSocketConnected();
    void onMessageReceived(String message);
    void onWebSocketClosed(String reason);
    void onError(Exception ex);
}