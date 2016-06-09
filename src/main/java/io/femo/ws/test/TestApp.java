package io.femo.ws.test;

import io.femo.http.Http;
import io.femo.http.handlers.FileHandler;
import io.femo.http.handlers.HttpDebugger;
import io.femo.http.handlers.LoggingHandler;
import io.femo.http.middleware.EnvironmentReplacerMiddleware;
import io.femo.ws.WebSocket;

import java.io.File;
import java.io.IOException;

/**
 * Created by felix on 6/4/16.
 */
public class TestApp {

    public static void main(String[] args) {
        Http.server(8080)
                .use("/", Http.router()
                        .get("/test", FileHandler.buffered(new File("test.html"), true, "text/html"))
                        .get("/script", FileHandler.resource("/websocket.js", false, "text/javascript"))
                        .use("/", Http.router()
                                .get("/", FileHandler.buffered(new File("index.html"), false, "text/html"))
                                .after(new EnvironmentReplacerMiddleware())
                        )
                        .after((request, response) -> {
                            response.header("Connection", "close");
                        })
                )
                .get("/ws", WebSocket.handler().handler((dataType, data, webSocketConnection) -> {
                    String msg = new String(data);
                    System.out.printf("Received message: %s\n", msg);
                    try {
                        webSocketConnection.send(msg);
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }
                    webSocketConnection.handler().sendToAll("Somebody connected");
                }))
                .get("/indexws", WebSocket
                        .on("test", (data, con) -> {
                            System.out.println(data.getAsString());
                            con.send("test", data);
                        })
                        .on("msg", (data, con) -> con.broadcast("msg", data, con))
                        .on("joined", (data, con) -> con.broadcast("joined", data, con))
                        .on("renamed", (data, con) -> con.broadcast("renamed", data, con))
                )
                .after(LoggingHandler.log())
                .start();
    }
}
