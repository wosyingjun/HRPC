package com.yingjun.rpc.service.impl;

import com.yingjun.rpc.annotation.HRPCService;
import com.yingjun.rpc.entity.Goods;
import com.yingjun.rpc.service.GoodsService;

/**
 * @author yingjun
 */
@HRPCService(GoodsService.class)
public class GoodsServiceImpl implements GoodsService {

    @Override
    public Goods getGoods(String title) {
        Goods goods = new Goods(title, 100F);
        return goods;
    }
}
