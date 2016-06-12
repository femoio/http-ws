package io.femo.ws.lib;

import io.femo.http.handlers.FileHandler;

/**
 * Created by felix on 6/11/16.
 */
public class LibraryScriptHandler extends FileHandler.ResourceFileHandler {


    public LibraryScriptHandler(boolean caching) {
        super(true, "text/javascript", "websocket.js");
    }
}
