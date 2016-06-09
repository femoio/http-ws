package io.femo.ws.lib;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.femo.http.HttpHandler;
import io.femo.ws.Constants;
import io.femo.ws.WebSocketConnection;
import io.femo.ws.WebSocketEventHandler;
import io.femo.ws.WebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Created by felix on 6/8/16.
 */
public class WebSocketLibraryHandler implements WebSocketEventHandler, Supplier<HttpHandler> {

    private static final Logger LOGGER = LoggerFactory.getLogger("HTTP-WS");
    private JsonParser jsonParser;
    private Map<String, List<Handler>> handlers;
    private WebSocketHandler parent;

    public WebSocketLibraryHandler(WebSocketHandler parent) {
        this.parent = parent;
        this.handlers = new HashMap<>();
    }

    @Override
    public void handleMessage(Constants.WEBSOCKET.FRAME.DataType dataType, byte[] data, WebSocketConnection webSocketConnection) {
        if(dataType == Constants.WEBSOCKET.FRAME.DataType.TEXT) {
            if (jsonParser == null) {
                jsonParser = new JsonParser();
            }
            String _json = new String(data);
            JsonElement jsonElement = jsonParser.parse(_json);
            if (jsonElement.isJsonObject()) {
                JsonObject sdata = jsonElement.getAsJsonObject();
                if (sdata.has("type") && sdata.get("type").isJsonPrimitive() && sdata.has("data")) {
                    String type = sdata.get("type").getAsString();
                    if (handlers.containsKey(type)) {
                        List<Handler> handlers = this.handlers.get(type);
                        handlers.forEach(h -> h.handle(sdata.get("data"), new WebSocketLibraryConnection(webSocketConnection)));
                    }
                } else {
                    LOGGER.warn("Received invalid frame on connection " + webSocketConnection.getName());
                    LOGGER.warn("Data");
                    LOGGER.warn(new String(data));
                }
            } else {
                LOGGER.warn("Received invalid frame on connection " + webSocketConnection.getName());
                LOGGER.warn("Data");
                LOGGER.warn(new String(data));
            }
        } else if (dataType != Constants.WEBSOCKET.FRAME.DataType.CLOSE) {
            LOGGER.warn("Received invalid frame on connection " + webSocketConnection.getName());
            LOGGER.warn("Type: " + dataType);
            LOGGER.warn("Data");
            LOGGER.warn(new String(data));
        }
    }

    @Override
    public HttpHandler get() {
        return parent;
    }

    @FunctionalInterface
    public interface Handler {

        void handle(JsonElement data, WebSocketLibraryConnection con);
    }

    public WebSocketLibraryHandler on(String type, Handler handler) {
        if(!handlers.containsKey(type)) {
            handlers.put(type, new ArrayList<>());
        }
        handlers.get(type).add(handler);
        return this;
    }

}
