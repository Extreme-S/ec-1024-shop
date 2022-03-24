package org.example.service;

import org.example.request.CartItemRequest;
import org.example.vo.CartItemVO;
import org.example.vo.CartVO;

import java.util.List;

public interface CartService {

    /**
     * 添加是商品到购物车
     */
    void addToCart(CartItemRequest cartItemRequest);

    /**
     * 清空购物车
     */
    void clear();

    /**
     * 查看我的购物车
     */
    CartVO getMyCart();

    /**
     * 删除购物项
     */
    void deleteItem(long productId);

    /**
     * 修改购物车商品数量
     */
    void changeItemNum(CartItemRequest cartItemRequest);

    /**
     * 确认购物车商品信息
     */
    List<CartItemVO> confirmOrderCartItems(List<Long> productIdList);
}
