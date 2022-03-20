package org.example.fegin;

import org.example.request.NewUserCouponRequest;
import org.example.util.JsonData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ec-coupon-service")
public interface CouponFeignService {

    /**
     * 新用户注册发放优惠券
     */
    @PostMapping("/api/coupon/v1/new_user_coupon")
    JsonData addNewUserCoupon(@RequestBody NewUserCouponRequest newUserCouponRequest);
}