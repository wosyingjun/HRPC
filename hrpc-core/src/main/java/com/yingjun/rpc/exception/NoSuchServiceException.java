package com.yingjun.rpc.exception;

/**
 * @author yingjun
 */
public class NoSuchServiceException extends RuntimeException{

    public NoSuchServiceException(String msg) {
        super(msg);
    }

}
