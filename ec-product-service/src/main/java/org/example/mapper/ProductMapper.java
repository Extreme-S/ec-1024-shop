package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.example.model.ProductDO;

public interface ProductMapper extends BaseMapper<ProductDO> {


    /**
     * 锁定商品库存
     */
    int lockProductStock(@Param("productId") long productId, @Param("buyNum") int buyNum);

    /**
     * 解锁商品存储
     */
    void unlockProductStock(@Param("productId") Long productId, @Param("buyNum") Integer buyNum);

}
