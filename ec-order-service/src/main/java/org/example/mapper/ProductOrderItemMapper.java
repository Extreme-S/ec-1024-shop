package org.example.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.model.ProductOrderItemDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

public interface ProductOrderItemMapper extends BaseMapper<ProductOrderItemDO> {

    /**
     * 批量插入
     */
    void insertBatch(@Param("orderItemList") List<ProductOrderItemDO> list);
}
