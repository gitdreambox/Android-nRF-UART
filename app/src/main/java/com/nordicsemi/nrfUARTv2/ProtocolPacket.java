package com.nordicsemi.nrfUARTv2;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import no.nordicsemi.android.log.ILogSession;
import no.nordicsemi.android.log.Logger;

/**
 * Created by dream on 2016/9/2.
 */
public class ProtocolPacket {
    /**
     * Characteristic value format type uint8
     */
    public static final int FORMAT_UINT8 = 0x11;

    /**
     * Characteristic value format type uint16
     */
    public static final int FORMAT_UINT16 = 0x12;

    /**
     * Characteristic value format type uint32
     */
    public static final int FORMAT_UINT32 = 0x14;

    /**
     * Characteristic value format type sint8
     */
    public static final int FORMAT_SINT8 = 0x21;

    /**
     * Characteristic value format type sint16
     */
    public static final int FORMAT_SINT16 = 0x22;

    /**
     * Characteristic value format type sint32
     */
    public static final int FORMAT_SINT32 = 0x24;

    /**
     * Characteristic value format type sfloat (16-bit float)
     */
    public static final int FORMAT_SFLOAT = 0x32;

    /**
     * Characteristic value format type float (32-bit float)
     */
    public static final int FORMAT_FLOAT = 0x34;
    byte[] mValue;
    int mValueOffset;
    int mRecvDataLen;

    int head;//0xA5
    int len;//len=Cmd size+Seq size+Payload size+Checksum size
    int cmd;//Cmd ID
    int seq;//Sequence
    byte[] payload;
    int offset;
    int checksum;//checksum
    Context context;
    protected GizwitsServiceCallbacks mCallbacks;

    public ProtocolPacket(Context context) {
        this.context = context;
        mValue = new byte[65536];
        mValueOffset = 0;
        mRecvDataLen = 0;
        this.head = 0xA5;
        this.len = 0;
        this.cmd = 0;
        this.seq = 0;
        this.payload = null;
        this.checksum = 0;
    }

    public ProtocolPacket(int head, int len, int cmd, int seq, byte[] payload, int checksum) {
        this.head = head;
        this.len = len;
        this.cmd = cmd;
        this.seq = seq;
        this.payload = payload;
        this.checksum = checksum;
    }

    public int getHead() {
        return head;
    }

    public void setHead(int head) {
        this.head = head;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public int getChecksum() {
        return checksum;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    private void process(byte[] b, int offset, int length) {
        int idx = offset;
        this.head = b[idx++] & 0xFF;
        this.len = ((b[idx++] & 0xFF) << 8) + (b[idx++] & 0xFF);
        this.cmd = b[idx++] & 0xFF;
        this.seq = b[idx++] & 0xFF;
        payload = new byte[offset + length - 5];
        System.arraycopy(b, offset + 5, payload, 0, offset + length - 5);
        this.checksum = b[offset + length] & 0xFF;
        int check = 0;
        for (int i = offset; i < offset + length; i++) {
            check += b[i] & 0xFF;
            check = check & 0xFF;
        }
        String log = "Recv Packet：\r\n" +
                "head=" + Integer.toHexString(head).toUpperCase() + "\r\n" +
                "len=" + Integer.toHexString(len).toUpperCase() + "\r\n" +
                "cmd=" + Integer.toHexString(cmd).toUpperCase() + "\r\n" +
                "seq=" + Integer.toHexString(seq).toUpperCase() + "\r\n" +
                "payload=" + hexUtils.bytesToHexString(payload, 0, payload.length) + "\r\n" +
                "checksum=" + Integer.toHexString(checksum).toUpperCase() + "\r\n";
        broadcastLog(log);
        Log.i("Packet", log);
        if (check == checksum) {
            switch (this.cmd) {
                case 0x01: {
                    int _offset = 0;
                    int statusCode=payload[_offset++]&0xFF;
                    String productKey = hexUtils.bytesToHexString(payload, _offset, 16);
                    _offset += 16;
                    String mac = hexUtils.bytesToHexString(payload, _offset, 6);
                    _offset += 6;
                    String hardwareVersion = hexUtils.bytesToString(payload, _offset, 8);
                    _offset += 8;
                    String softwareVersion = hexUtils.bytesToString(payload, _offset, 8);
                    _offset += 8;
                    String protocolVersion = hexUtils.bytesToString(payload, _offset, 8);
                    mCallbacks.SyncInfoResponse(productKey, mac, hardwareVersion, softwareVersion, protocolVersion);
                }

                break;
                case 0x02: {
                    int _offset = 0;
                    int statusCode = payload[_offset++] & 0xFF;
                    boolean encrypt = payload[_offset++] == 0 ? false : true;
                    int random = (payload[_offset++] & 0xFF) + ((payload[_offset++] & 0xFF) << 8) + ((payload[_offset++] & 0xFF) << 16) + ((payload[_offset++] & 0xFF) << 24);
                    mCallbacks.AuthResponse(encrypt, random);
                }
                    break;
                case 0x03: {
                    int statusCode = payload[0] & 0xFF;
                    mCallbacks.OTAResponse(statusCode);
                }
                    break;
                case 0x04:
                    String logRes = hexUtils.bytesToString(payload);
                    mCallbacks.LogResponse(logRes);
                    break;
                case 0x05:
                    mCallbacks.DataPointResponse(payload);
                    break;
                default:
                    break;
            }
        }
    }

    public void resetRecv() {
        mValueOffset = 0;
        mRecvDataLen = 0;
    }

    public void addRecvData(byte[] data) {
        boolean bool = false;
        Log.d("Packet", "addRecvData: " + hexUtils.bytesToHexString(data));
        if (mValueOffset < 5) {
            System.arraycopy(data, 0, this.mValue, this.mValueOffset, data.length);
            this.mValueOffset += data.length;
            bool = true;
        }
        if (mValueOffset >= 5) {
            if ((mValue[0] & 0xFF) == 0xA5) {
                mRecvDataLen = (((this.mValue[1] & 0xFF) << 8) + (this.mValue[2] & 0xFF)) + 2;
                if (mRecvDataLen > mValue.length) resetRecv();
                if (bool == false) {
                    System.arraycopy(data, 0, this.mValue, this.mValueOffset, data.length);
                    this.mValueOffset += data.length;
                }
                if (mValueOffset >= mRecvDataLen) {
                    //data packet receive done
                    process(mValue, 0, mRecvDataLen);
                    Log.d("Packet", "Packet: " + hexUtils.bytesToHexString(data));
                    resetRecv();
                }
            } else {
                resetRecv();
            }
        }
    }

    public byte[] getValue(int cmd) {
        checksum = 0;
        len = mValueOffset + 3;
        byte[] value = new byte[mValueOffset + 6];
        value[0] = (byte) (head & 0xFF);
        value[1] = (byte) (len >> 8 & 0xFF);
        value[2] = (byte) (len & 0xFF);
        value[3] = (byte) (cmd & 0xFF);
        value[4] = (byte) (seq & 0xFF);
        System.arraycopy(mValue, 0, value, 5, mValueOffset);
        for (int i = 0; i < value.length - 1; i++) {
            checksum += value[i];
        }
        value[value.length - 1] = (byte) (checksum & 0xFF);
        String log = "Send Packet：\r\n" +
                "head=" + Integer.toHexString(head).toUpperCase() + "\r\n" +
                "len=" + Integer.toHexString(len).toUpperCase() + "\r\n" +
                "cmd=" + Integer.toHexString(cmd).toUpperCase() + "\r\n" +
                "seq=" + Integer.toHexString(seq).toUpperCase() + "\r\n" +
                "payload=" + hexUtils.bytesToHexString(mValue, 0, mValueOffset) + "\r\n" +
                "checksum=" + Integer.toHexString(checksum & 0xFF).toUpperCase() + "\r\n";
        broadcastLog(log);
        Log.i("Packet", log);
        mValueOffset = 0;//reset
        return value;
    }

    public void setValue(String value) {
        byte[] b = value.getBytes();
        System.arraycopy(b, 0, mValue, mValueOffset, b.length);
        mValueOffset += b.length;
    }

    public void setValue(long value, int formatType) {
        switch (formatType) {
            case FORMAT_SINT8:
                mValue[mValueOffset++]=(byte)value;
//                value = intToSignedBits(value, 8);
//                mValue[mValueOffset++] = (byte)(value & 0xFF);
            case FORMAT_UINT8:
                mValue[mValueOffset++] = (byte) (value & 0xFF);
                break;
            case FORMAT_SINT16:
//                value = intToSignedBits(value, 16);
//                mValue[mValueOffset++] = (byte)(value & 0xFF);
//                mValue[mValueOffset++] = (byte)((value >> 8) & 0xFF);
            case FORMAT_UINT16:
                //Big Edian
                mValue[mValueOffset++] = (byte) ((value >> 8) & 0xFF);
                mValue[mValueOffset++] = (byte) (value & 0xFF);
                break;

            case FORMAT_SINT32:
//                value = intToSignedBits(value, 32);
//                mValue[mValueOffset++] = (byte)(value & 0xFF);
//                mValue[mValueOffset++] = (byte)((value >> 8) & 0xFF);
//                mValue[mValueOffset++] = (byte)((value >> 16) & 0xFF);
//                mValue[mValueOffset++] = (byte)((value >> 24) & 0xFF);
            case FORMAT_UINT32:
                //Big Edian
                mValue[mValueOffset++] = (byte) ((value >> 24) & 0xFF);
                mValue[mValueOffset++] = (byte) ((value >> 16) & 0xFF);
                mValue[mValueOffset++] = (byte) ((value >> 8) & 0xFF);
                mValue[mValueOffset++] = (byte) (value & 0xFF);
                break;

            default:
                break;
        }
    }

    public void setValue(byte[] value) {
        setValue(value, 0, value.length);
    }

    public void setValue(byte[] value, int offset, int length) {
        System.arraycopy(value, offset, mValue, mValueOffset, length);
        mValueOffset += length;
    }

    public void setGattCallbacks(GizwitsServiceCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    /**
     * Returns the size of a give value type.
     */
    private int getTypeLen(int formatType) {
        return formatType & 0xF;
    }

    /**
     * Convert a signed byte to an unsigned int.
     */
    private int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }

    /**
     * Convert signed bytes to a 16-bit unsigned int.
     */
    private int unsignedBytesToInt(byte b0, byte b1) {
        return (unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8));
    }

    /**
     * Convert signed bytes to a 32-bit unsigned int.
     */
    private int unsignedBytesToInt(byte b0, byte b1, byte b2, byte b3) {
        return (unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8))
                + (unsignedByteToInt(b2) << 16) + (unsignedByteToInt(b3) << 24);
    }

    /**
     * Convert signed bytes to a 16-bit short float value.
     */
    private float bytesToFloat(byte b0, byte b1) {
        int mantissa = unsignedToSigned(unsignedByteToInt(b0)
                + ((unsignedByteToInt(b1) & 0x0F) << 8), 12);
        int exponent = unsignedToSigned(unsignedByteToInt(b1) >> 4, 4);
        return (float) (mantissa * Math.pow(10, exponent));
    }

    /**
     * Convert signed bytes to a 32-bit short float value.
     */
    private float bytesToFloat(byte b0, byte b1, byte b2, byte b3) {
        int mantissa = unsignedToSigned(unsignedByteToInt(b0)
                + (unsignedByteToInt(b1) << 8)
                + (unsignedByteToInt(b2) << 16), 24);
        return (float) (mantissa * Math.pow(10, b3));
    }

    /**
     * Convert an unsigned integer value to a two's-complement encoded
     * signed value.
     */
    private int unsignedToSigned(int unsigned, int size) {
        if ((unsigned & (1 << size - 1)) != 0) {
            unsigned = -1 * ((1 << size - 1) - (unsigned & ((1 << size - 1) - 1)));
        }
        return unsigned;
    }

    /**
     * Convert an integer into the signed bits of a given length.
     */
    private int intToSignedBits(int i, int size) {
        if (i < 0) {
            i = (1 << size - 1) + (i & ((1 << size - 1) - 1));
        }
        return i;
    }

    public final static String ACTION_LOG = "com.nordicsemi.nrfLOG";
    public final static String EXTRA_DATA = "com.nordicsemi.nrfLOG.EXTRA_DATA";

    private void broadcastLog(final String log) {
        final Intent intent = new Intent(ACTION_LOG);
        intent.putExtra(EXTRA_DATA, log);
        LocalBroadcastManager.getInstance(this.context).sendBroadcast(intent);
    }
}
