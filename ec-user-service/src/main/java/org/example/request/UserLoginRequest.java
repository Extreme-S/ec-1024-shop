package org.example.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


@Data
@ApiModel(value = "登录对象", description = "用户登录请求对象")
public class UserLoginRequest {

    @ApiModelProperty(value = "邮箱", example = "794666918@qq.com")
    private String mail;

    @ApiModelProperty(value = "密码", example = "123456")
    private String pwd;

}
