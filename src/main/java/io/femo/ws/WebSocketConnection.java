package io.femo.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by felix on 6/4/16.
 */
public class WebSocketConnection extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger("HTTP-WS");

    private final InputStream inputStream;
    private final OutputStream outputStream;
    private WebSocketHandler webSocketHandler;
    private Socket socket;
    private AtomicBoolean open = new AtomicBoolean(true);
    private AtomicBoolean closing = new AtomicBoolean(false);

    public WebSocketConnection(WebSocketHandler webSocketHandler, Socket socket) throws IOException {
        this.webSocketHandler = webSocketHandler;
        this.socket = socket;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
    }

    @Override
    public void run() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        boolean frag = false;
        int opcode = 0;
        while (open.get()) {
            try {
                WebSocketFrame webSocketFrame = WebSocketFrame.read(inputStream);
                if(!webSocketFrame.isFin()) {
                    if(!frag && webSocketFrame.getOpcode() == 0) {
                        LOGGER.warn("Protocol violation. Fragmentation must be initialized with an opcode other than 0. " +
                                "See RFC 6455 Section 5.4");
                    }
                    if(frag && webSocketFrame.getOpcode() == 0) {
                        buffer.write(webSocketFrame.getApplicationData());
                    }
                    if(frag && (webSocketFrame.getOpcode() == Constants.WEBSOCKET.FRAME.OPCODES.TEXT_FRAME ||
                            webSocketFrame.getOpcode() == Constants.WEBSOCKET.FRAME.OPCODES.BINARY_FRAME)) {
                        LOGGER.error("Not recoverable protocol violation. Fragments of different messages must no be interleaved." +
                                "See RFC 6455 Section 5.4. Closing connection.");
                        close();
                    } else if (webSocketFrame.getOpcode() == Constants.WEBSOCKET.FRAME.OPCODES.TEXT_FRAME ||
                            webSocketFrame.getOpcode() == Constants.WEBSOCKET.FRAME.OPCODES.BINARY_FRAME) {
                        frag = true;
                        opcode = webSocketFrame.getOpcode();
                        buffer.write(webSocketFrame.getApplicationData());
                    } else {
                        LOGGER.warn("Protocol violation. Fragmentation can not be applied to control frames." +
                                "See RFC 6455 Section 5.4.");
                    }
                } else {
                    if(frag && webSocketFrame.getOpcode() == 0) {
                        buffer.write(webSocketFrame.getApplicationData());
                        frag = false;
                        webSocketHandler.raise(opcode, buffer.toByteArray(), this);
                        buffer.reset();
                    } else if(frag && (webSocketFrame.getOpcode() == Constants.WEBSOCKET.FRAME.OPCODES.TEXT_FRAME ||
                            webSocketFrame.getOpcode() == Constants.WEBSOCKET.FRAME.OPCODES.BINARY_FRAME)) {
                        LOGGER.error("Not recoverable protocol violation. Fragments of different messages must no be interleaved." +
                                "See RFC 6455 Section 5.4. Closing connection.");
                        close();
                    } else if (frag) {
                        webSocketHandler.raise(webSocketFrame.getOpcode(), webSocketFrame.getApplicationData(), this);
                    } else if (webSocketFrame.getOpcode() == 0) {
                        LOGGER.warn("Protocol violation. Continuation frames can only be sent during active fragmentation." +
                                "See RFC 6455 Section 5.4.");
                    } else {
                        if(webSocketFrame.getOpcode() == Constants.WEBSOCKET.FRAME.OPCODES.CONNECTION_CLOSE) {
                            if(!closing.get()) {
                                close();
                            }
                            open.set(false);
                            inputStream.close();
                            outputStream.close();
                            socket.close();
                            LOGGER.debug("Connection closed.");
                            webSocketHandler.raise(webSocketFrame.getOpcode(), webSocketFrame.getApplicationData(), this);
                        } else if (webSocketFrame.getOpcode() == Constants.WEBSOCKET.FRAME.OPCODES.PING) {
                            WebSocketFrame pongFrame = new WebSocketFrame();
                            pongFrame.setOpcode(Constants.WEBSOCKET.FRAME.OPCODES.PONG);
                            pongFrame.setFin(true);
                            pongFrame.setPayloadLength(webSocketFrame.getPayloadLength());
                            pongFrame.setApplicationData(webSocketFrame.getApplicationData());
                            synchronized (outputStream) {
                                pongFrame.write(outputStream);
                            }
                        } else {
                            webSocketHandler.raise(webSocketFrame.getOpcode(), webSocketFrame.getApplicationData(), this);
                        }

                    }
                }
            } catch (WebSocketException e) {
                open.set(false);
            } catch (IOException e) {
                LOGGER.warn("Error while handling WebSocket connection for " + socket.getInetAddress());
            }
        }
    }

    public void close () {
        close((short) 1000);
    }

    public void close(short statuscode) {
        WebSocketFrame webSocketFrame = new WebSocketFrame();
        webSocketFrame.setFin(true);
        webSocketFrame.setOpcode(Constants.WEBSOCKET.FRAME.OPCODES.CONNECTION_CLOSE);
        webSocketFrame.setPayloadLength(2);
        webSocketFrame.setApplicationData(ByteBuffer.allocate(2).putShort(statuscode).array());
        closing.set(true);
        synchronized (outputStream) {
            try {
                webSocketFrame.write(outputStream);
            } catch (IOException e) {
                LOGGER.error("Exception while closing the connection with status " + statuscode, e);
            }
        }
    }

    public void send(Constants.WEBSOCKET.FRAME.DataType dataType, byte[] data) throws IOException {
        WebSocketFrame webSocketFrame = new WebSocketFrame();
        webSocketFrame.setFin(true);
        if(dataType == Constants.WEBSOCKET.FRAME.DataType.BINARY) {
            webSocketFrame.setOpcode(Constants.WEBSOCKET.FRAME.OPCODES.BINARY_FRAME);
        } else if (dataType == Constants.WEBSOCKET.FRAME.DataType.TEXT) {
            webSocketFrame.setOpcode(Constants.WEBSOCKET.FRAME.OPCODES.TEXT_FRAME);
        }
        webSocketFrame.setPayloadLength(data.length);
        webSocketFrame.setApplicationData(data);
        synchronized (outputStream) {
            webSocketFrame.write(outputStream);
        }
    }

    public void send(String data) throws IOException {
        send(Constants.WEBSOCKET.FRAME.DataType.TEXT, data.getBytes("UTF-8"));
    }
}
