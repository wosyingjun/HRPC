package com.yingjun.rpc.proxy;

import com.yingjun.rpc.client.RPCClientHandler;
import com.yingjun.rpc.client.RPCFuture;
import com.yingjun.rpc.exception.NoSuchServiceException;
import com.yingjun.rpc.manage.ConnectManage;
import com.yingjun.rpc.protocol.RPCRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 同步调用代理对象
 */
public class RPCProxy<T> implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(RPCProxy.class);
    private Class<T> clazz;

    public RPCProxy(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * 同步调用方法
     *
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RPCRequest request = new RPCRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);
        logger.info("invoke class: {} method: {}", method.getDeclaringClass().getName(), method.getName());
        RPCClientHandler handler = ConnectManage.getInstance().chooseHandler(method.getDeclaringClass().getName());
        if(handler==null){
            logger.error("NoSuchServiceException:",
                    new NoSuchServiceException("no such service about"+method.getDeclaringClass().getName()));
            return null;
        }
        RPCFuture RPCFuture = handler.sendRequestBySync(request);
        return RPCFuture.get();
    }


}
