package org.example.mapper;

import org.example.model.CouponDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface CouponMapper extends BaseMapper<CouponDO> {

    /**
     * 扣减存储
     *
     * @param couponId
     * @return
     */
    int reduceStock(long couponId);
}
