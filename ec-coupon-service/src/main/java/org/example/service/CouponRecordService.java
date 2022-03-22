package org.example.service;


import org.example.model.CouponRecordMessage;
import org.example.request.LockCouponRecordRequest;
import org.example.util.JsonData;
import org.example.vo.CouponRecordVO;

import java.util.Map;

public interface CouponRecordService {

    /**
     * 分页查询领劵记录
     */
    Map<String, Object> page(int page, int size);

    /**
     * 根据id查询详情
     */
    CouponRecordVO findById(long recordId);

    /**
     * 锁定优惠券
     */
    JsonData lockCouponRecords(LockCouponRecordRequest recordRequest);


    /**
     * 释放优惠券记录
     */
    boolean releaseCouponRecord(CouponRecordMessage recordMessage);
}
