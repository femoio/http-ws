package io.femo.ws;

import io.femo.ws.lib.WebSocketLibraryConnection;
import io.femo.ws.lib.WebSocketLibraryHandler;
import org.jetbrains.annotations.Contract;

/**
 * Created by felix on 6/3/16.
 */
public final class WebSocket {

    @Contract(" -> !null")
    public static WebSocketHandler handler() {
        return new WebSocketHandler();
    }

    public static WebSocketLibraryHandler on(String type, WebSocketLibraryHandler.Handler handler) {
        return handler().library().on(type, handler);
    }

}
