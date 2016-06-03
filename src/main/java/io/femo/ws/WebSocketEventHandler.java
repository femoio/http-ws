package io.femo.ws;

/**
 * Created by felix on 6/3/16.
 */
@FunctionalInterface
public interface WebSocketEventHandler {

    void handleMessage(String type, String data);

}
