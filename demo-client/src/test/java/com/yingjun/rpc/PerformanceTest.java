package com.yingjun.rpc;

import com.yingjun.rpc.client.AsyncRPCCallback;
import com.yingjun.rpc.client.RPCClient;
import com.yingjun.rpc.client.RPCClientHandler;
import com.yingjun.rpc.proxy.AsyncRPCProxy;
import com.yingjun.rpc.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 客户端测试
 *
 * @author yingjun
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring-client.xml")
public class PerformanceTest {

    private static final Logger logger = LoggerFactory.getLogger(RPCClientHandler.class);
    @Autowired
    private RPCClient rpcClient;

    private int requestNum = 100000;
    private AtomicInteger successUmn = new AtomicInteger(0);
    private AtomicInteger failedNum = new AtomicInteger(0);

    /**
     * 性能测试
     */
    @Test
    public void testPerformance() {
        AsyncRPCProxy asyncProxy = rpcClient.createAsyncProxy(UserService.class);
        long startTime = System.currentTimeMillis();
        final CountDownLatch countDownLatch = new CountDownLatch(requestNum);
        for (int i = 1; i <= requestNum; i++) {
            asyncProxy.call("getUser", new AsyncRPCCallback() {
                @Override
                public void success(Object result) {
                    logger.info("result:" + result.toString());
                    successUmn.incrementAndGet();
                    countDownLatch.countDown();
                }

                @Override
                public void fail(Exception e) {
                    logger.error("result:" + e.getMessage());
                    failedNum.incrementAndGet();
                    countDownLatch.countDown();
                }
            }, String.valueOf(i));
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long useTime = System.currentTimeMillis() - startTime;
        logger.info("-------- {} request useTime:{} 毫秒", requestNum, useTime);
        logger.info("-------- {} request successUmn:{}", requestNum, successUmn.intValue());
        logger.info("-------- {} request failedNum:{}", requestNum, failedNum.intValue());

    }


}
