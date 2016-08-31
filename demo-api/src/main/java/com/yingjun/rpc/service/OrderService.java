package com.yingjun.rpc.service;

import com.yingjun.rpc.entity.Order;

import java.util.List;

/**
 *
 * @author yingjun
 */
public interface OrderService {

    public Order getOrder(String uuid);

    public List<Order> getOrderList(long userId);



}
