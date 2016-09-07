package com.nordicsemi.nrfUARTv2;

/**
 * Created by dream on 2016/9/7.
 */
public interface GizwitsServiceCallbacks {
    void SyncInfoResponse(String productKey, String mac, String hardwareVersion, String softwareVersion, String protocolVersion);

    void AuthResponse(boolean encrypt, int random);

    void OTAResponse(int statusCode);

    void LogResponse(String log);

    void DataPointResponse(byte[] data);
}
