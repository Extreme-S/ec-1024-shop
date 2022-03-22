package org.example.controller;


import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.example.enums.BizCodeEnum;
import org.example.request.LockCouponRecordRequest;
import org.example.service.CouponRecordService;
import org.example.util.JsonData;
import org.example.vo.CouponRecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/coupon_record/v1")
public class CouponRecordController {

    @Autowired
    private CouponRecordService couponRecordService;

    @ApiOperation("分页查询个人优惠券")
    @GetMapping("page")
    public JsonData page(
            @ApiParam(value = "当前页") @RequestParam(value = "page", defaultValue = "1") int page,
            @ApiParam(value = "每页显示多少条") @RequestParam(value = "size", defaultValue = "10") int size) {

        Map<String, Object> pageResult = couponRecordService.page(page, size);
        return JsonData.buildSuccess(pageResult);
    }

    @ApiOperation("查询优惠券记录详情")
    @GetMapping("detail/{record_id}")
    public JsonData getCouponRecordDetail(@ApiParam(value = "记录id") @PathVariable("record_id") long recordId) {
        CouponRecordVO couponRecordVO = couponRecordService.findById(recordId);
        return couponRecordVO == null
                ? JsonData.buildResult(BizCodeEnum.COUPON_NO_EXITS)
                : JsonData.buildSuccess(couponRecordVO);
    }

    @ApiOperation("rpc-锁定，优惠券记录")
    @PostMapping("lock_records")
    public JsonData lockCouponRecords(@ApiParam("锁定优惠券请求对象") @RequestBody LockCouponRecordRequest recordRequest) {
        return couponRecordService.lockCouponRecords(recordRequest);
    }


}

