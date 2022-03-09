package org.example.service;

import java.util.Map;

import org.example.enums.CouponCategoryEnum;
import org.example.request.NewUserCouponRequest;
import org.example.util.JsonData;

public interface CouponService {

    /**
     * 分页查询优惠券
     */
    Map<String, Object> pageCouponActivity(int page, int size);

    /**
     * 领取优惠券接口
     */
    JsonData addCoupon(long couponId, CouponCategoryEnum category);

    /**
     * 新用户注册发放优惠券
     */
    JsonData initNewUserCoupon(NewUserCouponRequest newUserCouponRequest);
}
