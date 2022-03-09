package org.example.service;

import org.example.vo.ProductVO;

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
}
