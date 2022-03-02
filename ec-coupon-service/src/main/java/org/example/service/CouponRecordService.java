package org.example.service;


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
}
