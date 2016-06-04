package io.femo.ws;

/**
 * Created by felix on 6/3/16.
 */
public final class Constants {

    public static final class WEBSOCKET {

        public static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        public static final String PROTO_NAME = "websocket";

        public static final class HEADERS {

            public static final String CONNECTION = "Connection";
            public static final String UPGRADE = "Upgrade";
            public static final String ORIGIN = "Origin";

            public static final String KEY = "Sec-WebSocket-Key";
            public static final String ACCEPT = "Sec-WebSocket-Accept";
            public static final String VERSION = "Sec-WebSocket-Version";
            public static final String PROTOCOL = "Sec-WebSocket-Protocol";
        }

        public static final class FRAME {

            public static final class OPCODES {

                public static final short CONTINUATION_FRAME = 0x0;
                public static final short TEXT_FRAME = 0x1;
                public static final short BINARY_FRAME = 0x2;
                public static final short CONNECTION_CLOSE = 0x8;
                public static final short PING = 0x9;
                public static final short PONG = 0xa;

            }

            public enum DataType {
                TEXT, BINARY, UNKNOWN
            }
        }
    }
}
