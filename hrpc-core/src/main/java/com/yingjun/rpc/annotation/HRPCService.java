package com.yingjun.rpc.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * RPC annotation for RPC test
 *
 * @author yingjun
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface HRPCService {
    Class<?> value();
}
