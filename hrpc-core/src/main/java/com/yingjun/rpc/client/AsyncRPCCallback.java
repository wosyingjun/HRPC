package com.yingjun.rpc.client;

/**
 * AsyncRPCCallback
 */
public interface AsyncRPCCallback {

    void success(Object result);

    void fail(Exception e);

}
