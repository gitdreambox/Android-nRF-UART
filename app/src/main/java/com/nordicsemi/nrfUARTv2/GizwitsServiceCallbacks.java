package com.nordicsemi.nrfUARTv2;

/**
 * Created by dream on 2016/9/7.
 */
public interface GizwitsServiceCallbacks {
    void Start(String productKey, String mac, int random);

    void AuthResponse(String sessionKey,boolean encrypt);

    void SyncInfoResponse(String hardwareVersion, String softwareVersion, String protocolVersion);

    void OTAResponse(int statusCode);

    void LogResponse(String log);

    void DataPointResponse(byte[] data);
}
