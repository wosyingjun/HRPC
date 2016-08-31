package com.yingjun.rpc;

import com.yingjun.rpc.client.AsyncRPCCallback;
import com.yingjun.rpc.client.RPCClient;
import com.yingjun.rpc.client.RPCClientHandler;
import com.yingjun.rpc.entity.Order;
import com.yingjun.rpc.entity.User;
import com.yingjun.rpc.proxy.AsyncRPCProxy;
import com.yingjun.rpc.service.OrderService;
import com.yingjun.rpc.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 客户端测试
 *
 * @author yingjun
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring-client.xml")
public class ClientBaseTest {

    private static final Logger logger = LoggerFactory.getLogger(RPCClientHandler.class);
    @Autowired
    private RPCClient rpcClient;

    /**
     * 初始化测试
     */
    @Test
    public void testInit() {
        while (true) {

        }
    }

    /**
     * 异步调用测试
     */
    @Test
    public void testInvokeByAsync() {
        AsyncRPCProxy asyncProxy = rpcClient.createAsyncProxy(UserService.class);

        logger.info("start invoke1!");
        asyncProxy.call("getUser", new AsyncRPCCallback() {
            @Override
            public void success(Object result) {
                logger.info("result:" + result.toString());
            }

            @Override
            public void fail(Exception e) {
                logger.error("result:" + e.getMessage());
            }
        }, "188888888");
        logger.info("finish invoke1!");

        logger.info("start invoke2!");
        asyncProxy.call("updateUser", new AsyncRPCCallback() {
            @Override
            public void success(Object result) {
                logger.info("result:" + result.toString());
            }

            @Override
            public void fail(Exception e) {
                logger.error("result:" + e.getMessage());
            }
        }, new User(111, "yingjun", "177777777"));
        logger.info("finish invoke12");

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 同步调用测试
     */
    @Test
    public void testInvokeBySync() {
        UserService userService = rpcClient.createProxy(UserService.class);
        User user1 = userService.getUser("188888888");
        logger.info("result:" + user1.toString());

        User user2 = userService.updateUser(user1);
        logger.info("result:" + user2.toString());

    }

    /**
     * 测试负载均衡
     */
    @Test
    public void testSLB() {
        OrderService orderService = rpcClient.createProxy(OrderService.class);
        for (int i = 0; i < 10; i++) {
            Order order = orderService.getOrder(String.valueOf(i));
            logger.info("result" + i + ":" + order.toString());
        }
    }


}
