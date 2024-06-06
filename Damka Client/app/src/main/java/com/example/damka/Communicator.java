package com.example.damka;



import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

/*
singleton class to allow easy spread through out the activities.
*/
public class Communicator  {
    private static final String serverURL = "ws://" + Constants.serverIP + ":" + Constants.serverPort;

    private static Communicator instance;
    private WebSocketClient webSocketClient;
    private WebSocketListener listener;

    private Communicator() {
        try {
            initializeWebSocketClient();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void initializeWebSocketClient() throws URISyntaxException {
        webSocketClient = new WebSocketClient(new URI(serverURL)) {
            @Override
            public void onOpen(ServerHandshake handshakeData) {
                if (listener != null) {
                    listener.onWebSocketConnected();
                }
            }

            @Override
            public void onMessage(String message) {
                if (listener != null) {
                    listener.onMessageReceived(message);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                if (listener != null) {
                    listener.onWebSocketClosed(reason);
                }
            }

            @Override
            public void onError(Exception ex) {
                if (listener != null) {
                    listener.onError(ex);
                }
            }
        };
    }

    public static Communicator getInstance() {
        if (instance == null) {
            instance = new Communicator();
        }
        return instance;
    }

    public void setListener(WebSocketListener listener) {
        this.listener = listener;
    }

    public void removeListener() {
        this.listener = null;
    }

    public void connect() {
        if (webSocketClient != null && !webSocketClient.isOpen()) {
            try {
                initializeWebSocketClient();
                webSocketClient.connect();
            } catch (Exception  e) {
                e.printStackTrace();
                webSocketClient.onError(e);
            }
        }
    }

    public void sendMessage(String message) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            try {
                webSocketClient.send(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void disconnect() {
        if (webSocketClient != null) {
            try {
                webSocketClient.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendCode(final int code) {
        String msg = "{\"code\": " + code + "}";
        this.sendMessage(msg);
    }

    public void sendCodeAndID(final int code, final String userID) {
        String msg = "{\"code\": " + code + ", \"userID\": \"" + userID + "\"}";
       this.sendMessage(msg);
    }

    public void sendLogin(final String mail, final String password) throws JSONException {
        JSONObject loginParams = new JSONObject();
        loginParams.put("code", Constants.LOGIN_CODE);
        loginParams.put("mail", mail);
        loginParams.put("password", password);

        this.sendMessage(loginParams.toString());
    }

    public void sendGetProfile(final String userID) {
        String msg = "{\"code\": " + Constants.GET_USER_PROFILE_CODE + ", \"userID\": \"" + userID + "\"}";
        this.sendMessage(msg);
    }
}