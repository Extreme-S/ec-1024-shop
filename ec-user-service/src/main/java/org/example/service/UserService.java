package org.example.service;

import org.example.request.UserRegisterRequest;
import org.example.util.JsonData;

public interface UserService {

    /**
     * 用户注册
     *
     * @param registerRequest
     * @return
     */
    JsonData register(UserRegisterRequest registerRequest);

}
