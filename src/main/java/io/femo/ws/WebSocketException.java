package io.femo.ws;

import java.io.IOException;

/**
 * Created by felix on 6/4/16.
 */
public class WebSocketException extends IOException {
    public WebSocketException(String s) {
        super(s);
    }
}
