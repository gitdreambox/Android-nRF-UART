package com.nordicsemi.nrfUARTv2;

import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Dream on 2016/11/3.
 */

public class AES {
    private final static String TAG = AES.class.getSimpleName();
    public static byte[] KEY={0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0A,0x0B,0x0C,0x0D,0x0E,0x0F};
    public static byte[] Encrypt(byte[] buf,byte[] key)
    {
        try {
            Log.d(TAG,"Encrypt src"+hexUtils.bytesToHexString(buf));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            int blockSize = cipher.getBlockSize();
            SecretKeySpec keyspec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivspec = new IvParameterSpec(key);
            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
            byte[] encrypted = cipher.doFinal(buf);
            Log.d(TAG,"Encrypt des"+hexUtils.bytesToHexString(encrypted));
            return encrypted;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static byte[] Decrypt(byte[] buf,byte[] key)
    {
        try
        {
            Log.d(TAG,"Decrypt src"+hexUtils.bytesToHexString(buf));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keyspec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivspec = new IvParameterSpec(key);
            cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);
            byte[] original = cipher.doFinal(buf);
            Log.d(TAG,"Decrypt des"+hexUtils.bytesToHexString(original));
            return original;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static boolean Test()
    {
        byte[] en_out=new byte[32];
        byte[] de_out=new byte[32];
        byte[] in={0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0A,0x0B,0x0C,0x0D,0x0E,0x0F,0x00,0x01};
        byte[] key={0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0A,0x0B,0x0C,0x0D,0x0E,0x0F};
        en_out=AES.Encrypt(in,key);
        Log.d("AES",hexUtils.bytesToHexString(en_out));
        de_out=AES.Decrypt(en_out,key);
        Log.d("AES",hexUtils.bytesToHexString(de_out));
        for(int i=0;i<18;i++)
        {
            if(de_out[i]!=in[i])
            {
                return false;
            }
        }
        return true;
    }
}
