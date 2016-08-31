package com.yingjun.rpc.client;

import com.yingjun.rpc.manage.ConnectManage;
import com.yingjun.rpc.proxy.AsyncRPCProxy;
import com.yingjun.rpc.proxy.RPCProxy;
import com.yingjun.rpc.registry.ServiceDiscovery;

import java.lang.reflect.Proxy;
import java.util.List;

/**
 * @author yingjun
 */
public class RPCClient {

    private ServiceDiscovery serviceDiscovery;

    public RPCClient(String zookeeper, List<String> interfaces) {
        this.serviceDiscovery = new ServiceDiscovery(zookeeper, interfaces);
    }

    /**
     * 创建用于同步调用的代理对象
     *
     * @param interfaceClass
     * @param <T>
     * @return
     */
    public static <T> T createProxy(Class<T> interfaceClass) {
        // 创建动态代理对象
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new RPCProxy<T>(interfaceClass)
        );
    }
    /**
     * 创建用于异步调用的代理对象
     *
     * @param interfaceClass
     * @param <T>
     * @return
     */
    public static <T> AsyncRPCProxy createAsyncProxy(Class<T> interfaceClass) {
        return new AsyncRPCProxy<T>(interfaceClass);
    }


    public void stop() {
        serviceDiscovery.stop();
        ConnectManage.getInstance().stop();
    }

}
