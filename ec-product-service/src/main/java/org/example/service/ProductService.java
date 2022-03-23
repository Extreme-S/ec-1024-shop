package org.example.service;

import org.example.model.ProductMessage;
import org.example.request.LockProductRequest;
import org.example.util.JsonData;
import org.example.vo.ProductVO;

import java.util.List;
import java.util.Map;

public interface ProductService {

    /**
     * 分页查询商品列表
     */
    Map<String, Object> page(int page, int size);

    /**
     * 根据id找商品详情
     */
    ProductVO findDetailById(long productId);

    /**
     * 根据id批量查询商品
     */
    List<ProductVO> findProductsByIdBatch(List<Long> productIdList);

    /**
     * 锁定商品库存
     */
    JsonData lockProductStock(LockProductRequest lockProductRequest);

    /**
     * 释放商品库存
     */
    boolean releaseProductStock(ProductMessage productMessage);
}
