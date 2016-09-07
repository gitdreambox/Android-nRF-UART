package com.nordicsemi.nrfUARTv2;

import java.io.UnsupportedEncodingException;

/**
 * Created by dream on 2016/9/2.
 */
public class hexUtils {

    private static  byte toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }
    public static  byte[]  hexStringToBytes(String hex)
    {
        int len = (hex.length() / 2);
        byte[] result = new byte[len];
        char[] achar = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
        }
        return result;

    }


    public static   String bytesToHexString(byte[] b) {
        return bytesToHexString(b,0,b.length);
    }
    public static   String bytesToHexString(byte[] b,int offset,int length) {
        String a = "";
        int len=offset+length;
        len=len>b.length?b.length:len;
        for (int i = offset; i < len; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF).toUpperCase();
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            a = a+hex;
        }
        return a;
    }
    public static   String bytesToString(byte[] b) {
        return bytesToString(b,0,b.length);
    }
    public static   String bytesToString(byte[] b,int offset,int length) {
        String ret="";
        try {
            ret= new String(b,offset,length,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ret;
    }
}
