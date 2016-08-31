package com.yingjun.rpc.service.impl;

import com.yingjun.rpc.annotation.HRPCService;
import com.yingjun.rpc.client.RPCClientHandler;
import com.yingjun.rpc.entity.Order;
import com.yingjun.rpc.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yingjun
 */
@HRPCService(OrderService.class)
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(RPCClientHandler.class);

    @Override
    public Order getOrder(String uuid) {
        //方便做负载均衡测试，这里做个数据打印
        Order order = new Order(uuid, 111, "service2", 15.2F);
        return order;
    }

    @Override
    public List<Order> getOrderList(long userId) {
        List<Order> list = new ArrayList<Order>();
        for (int i = 0; i < 20; i++) {
            Order order = new Order("uuid@" + i, 111, "service2@" + i, i + 0.1F);
            list.add(order);
        }
        return list;
    }
}
