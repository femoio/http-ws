package io.femo.ws;

import io.femo.http.drivers.InputBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by felix on 6/4/16.
 */
public class WebSocketFrame {

    private boolean fin;

    private boolean rsv1;
    private boolean rsv2;
    private boolean rsv3;

    private short opcode;

    private boolean mask;

    private long payloadLength;

    private byte[] maskingKey;

    private byte[] applicationData;

    public boolean isFin() {
        return fin;
    }

    public void setFin(boolean fin) {
        this.fin = fin;
    }

    public boolean isRsv1() {
        return rsv1;
    }

    public void setRsv1(boolean rsv1) {
        this.rsv1 = rsv1;
    }

    public boolean isRsv2() {
        return rsv2;
    }

    public void setRsv2(boolean rsv2) {
        this.rsv2 = rsv2;
    }

    public boolean isRsv3() {
        return rsv3;
    }

    public void setRsv3(boolean rsv3) {
        this.rsv3 = rsv3;
    }

    public short getOpcode() {
        return opcode;
    }

    public void setOpcode(short opcode) {
        this.opcode = opcode;
    }

    public boolean isMask() {
        return mask;
    }

    public void setMask(boolean mask) {
        this.mask = mask;
    }

    public long getPayloadLength() {
        return payloadLength;
    }

    public void setPayloadLength(long payloadLength) {
        this.payloadLength = payloadLength;
    }

    public byte[] getMaskingKey() {
        return maskingKey;
    }

    public void setMaskingKey(byte[] maskingKey) {
        this.maskingKey = maskingKey;
    }

    public byte[] getApplicationData() {
        return applicationData;
    }

    public void setApplicationData(byte[] applicationData) {
        this.applicationData = applicationData;
    }

    public void write(OutputStream outputStream) throws IOException {
        byte data = (byte) (fin ? 0b10000000 : 0);
        data |= rsv1 ? 0b01000000 : 0;
        data |= rsv2 ? 0b00100000 : 0;
        data |= rsv3 ? 0b00010000 : 0;
        data |= opcode & 0b1111;
        outputStream.write(data);
        data = (byte) (mask ? 0b10000000 : 0);
        if(payloadLength < 126) {
            data |= payloadLength;
            outputStream.write(data);
        } else if (payloadLength < Short.MAX_VALUE) {
            data |= 126;
            outputStream.write(data);
            ByteBuffer byteBuffer = ByteBuffer.allocate(2);
            byteBuffer.putShort((short) payloadLength);
            outputStream.write(byteBuffer.array(), 0, 2);
        } else {
            data |= 127;
            outputStream.write(data);
            ByteBuffer byteBuffer = ByteBuffer.allocate(8);
            byteBuffer.putLong(payloadLength);
            outputStream.write(byteBuffer.array(), 0, 8);
        }
        if(mask) {
            outputStream.write(maskingKey, 0, 4);
            maskData();
        }
        if(payloadLength > 0) {
            outputStream.write(applicationData);
        }
    }

    public static WebSocketFrame read(InputStream inputStream) throws IOException {
        WebSocketFrame wsf = new WebSocketFrame();
        int data = inputStream.read();
        if(data == -1) {
            throw new WebSocketException("Connection closed!");
        }
        wsf.fin = (data & 0b10000000) == 0b10000000;
        wsf.rsv1 = (data & 0b01000000) == 0b01000000;
        wsf.rsv2 = (data & 0b00100000) == 0b00100000;
        wsf.rsv3 = (data & 0b00010000) == 0b00010000;
        wsf.opcode = (short) (data & 0b1111);
        data = inputStream.read();
        if(data == -1) {
            throw new WebSocketException("Connection closed!");
        }
        wsf.mask = (data & 0b10000000) == 0b10000000;
        int tmp = data & 0b1111111;
        InputBuffer inputBuffer = new InputBuffer(inputStream);
        if(tmp < 126) {
            wsf.payloadLength = tmp;
        } else if (tmp == 126) {
            byte[] tData = inputBuffer.get(2);
            ByteBuffer byteBuffer = ByteBuffer.wrap(tData);
            wsf.payloadLength = byteBuffer.getShort();
        } else {
            byte[] tData = inputBuffer.get(8);
            ByteBuffer byteBuffer = ByteBuffer.wrap(tData);
            wsf.payloadLength = byteBuffer.getLong();
        }
        if(wsf.mask) {
            wsf.maskingKey = inputBuffer.get(4);
        }
        wsf.applicationData = inputBuffer.get((int) wsf.payloadLength);
        if(wsf.mask) {
            wsf.maskData();
        }
        return wsf;
    }

    private void maskData() {
        for (int i = 0; i < applicationData.length; i++) {
            applicationData[i] = (byte) (applicationData[i] ^ maskingKey[i % 4]);
        }
    }


}
