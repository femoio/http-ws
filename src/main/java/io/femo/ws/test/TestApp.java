package io.femo.ws.test;

import io.femo.http.Http;
import io.femo.ws.WebSocket;

import java.io.IOException;

/**
 * Created by felix on 6/4/16.
 */
public class TestApp {

    public static void main(String[] args) {
        Http.server(8080)
                .get("/ws", WebSocket.handler().handler((dataType, data, webSocketConnection) -> {
                    String msg = new String(data);
                    System.out.println(msg);
                    try {
                        webSocketConnection.send(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                })).start();
    }
}
