package com.yingjun.rpc.service;

import com.yingjun.rpc.entity.User;

/**
 * @author yingjun
 */
public interface UserService {

    public User getUser(String phone);

    public User updateUser(User user);

}
