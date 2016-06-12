package io.femo.ws;

import io.femo.http.*;
import io.femo.http.Http;
import io.femo.http.helper.HttpHelper;
import io.femo.ws.lib.WebSocketLibraryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by felix on 6/3/16.
 */
public class WebSocketHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("HTTP-WS");

    private List<WebSocketEventHandler> webSocketEventHandlers;
    private List<WebSocketConnection> connections;

    private MessageDigest messageDigest;

    WebSocketHandler() {
        try {
            this.messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Message Digest not found. This system is not compatible", e);
        }
        this.webSocketEventHandlers = new ArrayList<>();
        this.connections = new ArrayList<>();
    }

    public boolean handle(HttpRequest request, HttpResponse response) throws HttpHandleException {
        if (messageDigest == null) {
            return false;
        }
        if (Objects.equals(request.method(), Http.GET) &&
                request.hasHeaders(
                        Constants.WEBSOCKET.HEADERS.UPGRADE,
                        Constants.WEBSOCKET.HEADERS.KEY,
                        Constants.WEBSOCKET.HEADERS.CONNECTION,
                        Constants.WEBSOCKET.HEADERS.VERSION
                )) {
            if (request.header(Constants.WEBSOCKET.HEADERS.CONNECTION).value().contains(Constants.WEBSOCKET.HEADERS.UPGRADE)) {
                if (request.header(Constants.WEBSOCKET.HEADERS.UPGRADE).value().contains(Constants.WEBSOCKET.PROTO_NAME) &&
                        request.header(Constants.WEBSOCKET.HEADERS.VERSION).asInt() == 13) {
                    String accept = request.header(Constants.WEBSOCKET.HEADERS.KEY).value() + Constants.WEBSOCKET.GUID;
                    try {
                        response.header(Constants.WEBSOCKET.HEADERS.ACCEPT, DatatypeConverter
                                .printBase64Binary(messageDigest.digest(accept.getBytes("UTF-8"))))
                            .status(101)
                            .header(Constants.WEBSOCKET.HEADERS.UPGRADE, Constants.WEBSOCKET.PROTO_NAME)
                            .header(Constants.WEBSOCKET.HEADERS.CONNECTION, Constants.WEBSOCKET.HEADERS.UPGRADE)
                            .header("Content-Length", "0");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    messageDigest.reset();
                    Socket socket = HttpHelper.get().getFirst(Socket.class).get();
                    LOGGER.debug("New WebSocket Client connecting from [{}]:{} using version {} [{}]",
                            socket.getInetAddress().toString(), socket.getPort(), request.header(Constants.WEBSOCKET.HEADERS.VERSION),
                            request.header(Constants.WEBSOCKET.HEADERS.PROTOCOL));
                    HttpHelper.keepOpen();
                    HttpHelper.callback(() -> {
                        try {
                            WebSocketConnection webSocketConnection = new WebSocketConnection(this, socket);
                            webSocketConnection.start();
                            connections.add(webSocketConnection);
                            webSocketConnection.ping("Hello World");
                        } catch (IOException e) {
                            LOGGER.warn("Could not establish WebSocket connection", e);
                        }
                    });
                } else {
                    LOGGER.warn("Invalid upgrade request received " + request.requestLine());
                    response.status(StatusCode.BAD_REQUEST);
                    response.entity("Invalid Upgrade Protocol selected. Only websocket supported so far...");
                }
            }
        } else {
            response.status(404);
            response.entity("The requested resource could not be found!");
        }
        return true;
    }

    public WebSocketHandler handler(WebSocketEventHandler handler) {
        this.webSocketEventHandlers.add(handler);
        return this;
    }

    synchronized void raise(int opcode, byte[] bytes, WebSocketConnection webSocketConnection) {
        Constants.WEBSOCKET.FRAME.DataType dataType;
        if(opcode == Constants.WEBSOCKET.FRAME.OPCODES.BINARY_FRAME) {
            dataType = Constants.WEBSOCKET.FRAME.DataType.BINARY;
        } else if (opcode == Constants.WEBSOCKET.FRAME.OPCODES.TEXT_FRAME) {
            dataType = Constants.WEBSOCKET.FRAME.DataType.TEXT;
        } else if (opcode == Constants.WEBSOCKET.FRAME.OPCODES.CONNECTION_CLOSE) {
            dataType = Constants.WEBSOCKET.FRAME.DataType.CLOSE;
            connections.remove(webSocketConnection);
        } else {
            dataType = Constants.WEBSOCKET.FRAME.DataType.UNKNOWN;
        }
        this.webSocketEventHandlers.forEach(w -> w.handleMessage(dataType, bytes, webSocketConnection));
    }

    public void sendToAll(String message) {
        sendToAll(Constants.WEBSOCKET.FRAME.DataType.TEXT, message.getBytes());
    }

    public synchronized void sendToAll(Constants.WEBSOCKET.FRAME.DataType dataType, byte[] data) {
        connections.removeIf(WebSocketConnection::isClosed);
        connections.forEach(c -> {
            try {
                c.send(dataType, data);
            } catch (IOException e) {
                LOGGER.warn("Error while sending data to connection!", e);
            }
        });
    }

    public WebSocketLibraryHandler library() {
        WebSocketLibraryHandler handler = new WebSocketLibraryHandler(this);
        handler(handler);
        return handler;
    }

    public void sendToAllExcept(Constants.WEBSOCKET.FRAME.DataType dataType, byte[] data, List<WebSocketConnection> but) {
        connections.stream().filter(c -> !but.stream().anyMatch(ws -> ws.getName().equals(c.getName())))
                .forEach(c -> {
                    try {
                        c.send(dataType, data);
                    } catch (IOException e) {
                        LOGGER.warn("Error while broadcasting data! Connection probably already closed!");
                        c.close();
                    }
                });
    }
}
