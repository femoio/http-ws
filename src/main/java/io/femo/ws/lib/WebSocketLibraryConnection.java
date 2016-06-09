package io.femo.ws.lib;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.femo.ws.WebSocketConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by felix on 6/8/16.
 */
public class WebSocketLibraryConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger("HTTP-WS");

    private WebSocketConnection connection;

    public WebSocketLibraryConnection(WebSocketConnection connection) {
        this.connection = connection;
    }

    public WebSocketLibraryConnection send(String type, JsonElement data) {
        try {
            connection.send(buildData(type, data).toString());
        } catch (IOException e) {
            LOGGER.warn("Potentially faulty connection. Closing!", e);
            connection.close();
        }
        return this;
    }

    public void broadcast(String type, JsonElement data, WebSocketLibraryConnection... but) {
        connection.broadcastExcept(buildData(type, data).toString(),
                Arrays.asList(but).stream().map(WebSocketLibraryConnection::getConnection)
                        .collect(Collectors.toList()));
    }

    private WebSocketConnection getConnection() {
        return connection;
    }

    private JsonObject buildData(String type, JsonElement data) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", type);
        jsonObject.add("data", data);
        return jsonObject;
    }
}
