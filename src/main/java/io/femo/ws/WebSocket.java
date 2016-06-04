package io.femo.ws;

import org.jetbrains.annotations.Contract;

/**
 * Created by felix on 6/3/16.
 */
public final class WebSocket {

    @Contract(" -> !null")
    public static WebSocketHandler handler() {
        return new WebSocketHandler();
    }

}
