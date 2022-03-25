package org.example.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.model.ProductOrderDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface ProductOrderMapper extends BaseMapper<ProductOrderDO> {

    /**
     * 更新订单状态
     */
    void updateOrderPayState(@Param("outTradeNo") String outTradeNo,
                             @Param("newState") String newState,
                             @Param("oldState") String oldState);
}
