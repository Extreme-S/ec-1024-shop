package org.example.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.example.enums.CouponCategoryEnum;
import org.example.service.CouponService;
import org.example.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api("优惠券模块")
@RestController
@RequestMapping("/api/coupon/v1")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @ApiOperation("分页查询优惠券")
    @GetMapping("page_coupon")
    public JsonData pageCouponList(
            @ApiParam(value = "当前页") @RequestParam(value = "page", defaultValue = "1") int page,
            @ApiParam(value = "每页显示多少条") @RequestParam(value = "size", defaultValue = "10") int size) {

        Map<String, Object> pageMap = couponService.pageCouponActivity(page, size);
        return JsonData.buildSuccess(pageMap);
    }
    
    /**
     * 领取优惠券
     */
    @ApiOperation("领取优惠券")
    @GetMapping("/add/promotion/{coupon_id}")
    public JsonData addPromotionCoupon(
            @ApiParam(value = "优惠券id", required = true) @PathVariable("coupon_id") long couponId) {

        return (JsonData) couponService.addCoupon(couponId, CouponCategoryEnum.PROMOTION);
    }


}

