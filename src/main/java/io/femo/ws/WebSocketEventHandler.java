package io.femo.ws;

/**
 * Created by felix on 6/3/16.
 */
@FunctionalInterface
public interface WebSocketEventHandler {

    void handleMessage(Constants.WEBSOCKET.FRAME.DataType dataType, byte[] data, WebSocketConnection webSocketConnection);

}
