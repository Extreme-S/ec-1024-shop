package org.example.service;

import org.example.request.UserLoginRequest;
import org.example.request.UserRegisterRequest;
import org.example.util.JsonData;
import org.example.vo.UserVO;

public interface UserService {

    /**
     * 用户注册
     */
    JsonData register(UserRegisterRequest registerRequest);

    /**
     * 用户登录
     */
    JsonData login(UserLoginRequest userLoginRequest);

    /**
     * 查询用户详情
     */
    UserVO findUserDetail();
}
