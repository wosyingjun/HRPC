package com.yingjun.rpc.service.impl;

import com.yingjun.rpc.annotation.HRPCService;
import com.yingjun.rpc.entity.User;
import com.yingjun.rpc.service.UserService;

/**
 * @author yingjun
 */
@HRPCService(UserService.class)
public class UserServiceImpl implements UserService {

    @Override
    public User getUser(String phone) {
        User user =new User(111,"yingjun",phone);
        return user;
    }

    @Override
    public User updateUser(User user) {
        user.setName("yingjun@update");
        return user;
    }
}
