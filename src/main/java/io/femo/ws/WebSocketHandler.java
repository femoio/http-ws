package io.femo.ws;

import io.femo.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Created by felix on 6/3/16.
 */
public class WebSocketHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("HTTP-WS");

    MessageDigest messageDigest;

    public WebSocketHandler() {
        try {
            this.messageDigest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Message Digest not found. This system is not compatible", e);
        }
    }

    public boolean handle(HttpRequest request, HttpResponse response) throws HttpHandleException {
        if(messageDigest == null) {
            return false;
        }
        if(request.header("Connection").equals("Upgrade")) {
            if(request.header("Upgrade").equals("websocket")) {
                if(request.hasHeader("Sec-WebSocket-Key")) {
                    messageDigest.update(request.header("Sec-WebSocket-Key").value().getBytes());
                    messageDigest.update(Constants.WEBSOCKET.GUID.getBytes());
                    response.header("Sec-WebSocket-Accept", Base64.getEncoder().encodeToString(messageDigest.digest()));
                    response.status(101);
                    response.header("Upgrade", "websocket");
                    response.header("Connection", "Upgrade");
                    messageDigest.reset();
                } else {
                    response.status(StatusCode.BAD_REQUEST);
                    response.entity("Invalid handshake");
                }
            } else {
                response.status(StatusCode.BAD_REQUEST);
                response.entity("Invalid Upgrade Protocol selected. Only websocket supported so far...");
            }
        } else {
            response.status(404);
            response.entity("The requested resource could not be found!");
        }
        return true;
    }
}
