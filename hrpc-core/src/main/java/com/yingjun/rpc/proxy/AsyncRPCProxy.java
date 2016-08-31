package com.yingjun.rpc.proxy;

import com.yingjun.rpc.client.AsyncRPCCallback;
import com.yingjun.rpc.client.RPCClientHandler;
import com.yingjun.rpc.client.RPCFuture;
import com.yingjun.rpc.manage.ConnectManage;
import com.yingjun.rpc.protocol.RPCRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * 异步调用代理对象
 */
public class AsyncRPCProxy<T> {

    private static final Logger logger = LoggerFactory.getLogger(RPCProxy.class);
    private Class<T> clazz;

    public AsyncRPCProxy(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * 异步调用方法
     * @param funcName
     * @param args
     * @return
     */
    public RPCFuture call(String funcName, AsyncRPCCallback callback, Object... args) {
        RPCRequest request = new RPCRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(this.clazz.getName());
        request.setMethodName(funcName);
        request.setParameters(args);

        Class[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = getClassType(args[i]);
        }
        request.setParameterTypes(parameterTypes);
        logger.info("invoke class: {} method: {}", this.clazz.getName(), funcName);

        RPCClientHandler handler = ConnectManage.getInstance().chooseHandler(this.clazz.getName());
        RPCFuture RPCFuture = handler.sendRequestByAsync(request, callback);
        return RPCFuture;
    }


    //基本类型转换
    private Class<?> getClassType(Object obj) {
        Class<?> classType = obj.getClass();
        String typeName = classType.getName();
        if ("java.lang.Integer".equals(typeName)) {
            return Integer.TYPE;
        } else if ("java.lang.Long".equals(typeName)) {
            return Long.TYPE;
        } else if ("java.lang.Float".equals(typeName)) {
            return Float.TYPE;
        } else if ("java.lang.Double".equals(typeName)) {
            return Double.TYPE;
        } else if ("java.lang.Character".equals(typeName)) {
            return Character.TYPE;
        } else if ("java.lang.Boolean".equals(typeName)) {
            return Boolean.TYPE;
        } else if ("java.lang.Short".equals(typeName)) {
            return Short.TYPE;
        } else if ("java.lang.Byte".equals(typeName)) {
            return Byte.TYPE;
        }
        return classType;
    }


}
