package org.example.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.model.CouponRecordDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

public interface CouponRecordMapper extends BaseMapper<CouponRecordDO> {

    /**
     * 批量更新优惠券使用记录
     */
    int lockUseStateBatch(@Param("userId") Long userId,
                          @Param("useState") String useState,
                          @Param("lockCouponRecordIds") List<Long> lockCouponRecordIds);


    /**
     * 更新优惠券使用记录
     */
    void updateState(@Param("couponRecordId") Long couponRecordId,
                     @Param("useState") String useState);
}
