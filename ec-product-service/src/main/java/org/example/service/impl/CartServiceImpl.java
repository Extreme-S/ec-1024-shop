package org.example.service.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.example.constant.CacheKey;
import org.example.enums.BizCodeEnum;
import org.example.exception.BizException;
import org.example.interceptor.LoginInterceptor;
import org.example.model.LoginUser;
import org.example.request.CartItemRequest;
import org.example.service.CartService;
import org.example.service.ProductService;
import org.example.vo.CartItemVO;
import org.example.vo.CartVO;
import org.example.vo.ProductVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Slf4j
public class CartServiceImpl implements CartService {

    @Autowired
    private ProductService productService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void addToCart(CartItemRequest cartItemRequest) {
        long productId = cartItemRequest.getProductId();
        int buyNum = cartItemRequest.getBuyNum();
        //获取购物车
        BoundHashOperations<String, Object, Object> myCart = getMyCartOps();

        Object cacheObj = myCart.get(productId);
        String result = "";
        if (cacheObj != null) {
            result = (String) cacheObj;
        }
        if (StringUtils.isBlank(result)) {
            //不存在则新建一个商品
            CartItemVO cartItemVO = new CartItemVO();
            ProductVO productVO = productService.findDetailById(productId);
            if (productVO == null) {
                throw new BizException(BizCodeEnum.CART_FAIL);
            }

            cartItemVO.setAmount(productVO.getAmount());
            cartItemVO.setBuyNum(buyNum);
            cartItemVO.setProductId(productId);
            cartItemVO.setProductImg(productVO.getCoverImg());
            cartItemVO.setProductTitle(productVO.getTitle());
            myCart.put(productId, JSON.toJSONString(cartItemVO));
        } else {
            //存在商品，修改数量
            CartItemVO cartItem = JSON.parseObject(result, CartItemVO.class);
            cartItem.setBuyNum(cartItem.getBuyNum() + buyNum);
            myCart.put(productId, JSON.toJSONString(cartItem));
        }
    }


    /**
     * 清空购物车
     */
    @Override
    public void clear() {
        String cartKey = getCartKey();
        redisTemplate.delete(cartKey);

    }


    /**
     * 删除购物项
     */
    @Override
    public void deleteItem(long productId) {
        BoundHashOperations<String, Object, Object> mycart = getMyCartOps();
        mycart.delete(productId);
    }


    @Override
    public void changeItemNum(CartItemRequest cartItemRequest) {
        BoundHashOperations<String, Object, Object> mycart = getMyCartOps();
        Object cacheObj = mycart.get(cartItemRequest.getProductId());
        if (cacheObj == null) {
            throw new BizException(BizCodeEnum.CART_FAIL);
        }
        String obj = (String) cacheObj;
        CartItemVO cartItemVO = JSON.parseObject(obj, CartItemVO.class);
        cartItemVO.setBuyNum(cartItemRequest.getBuyNum());
        mycart.put(cartItemRequest.getProductId(), JSON.toJSONString(cartItemVO));
    }


    @Override
    public CartVO getMyCart() {

        //获取全部购物项
        List<CartItemVO> cartItemVOList = buildCartItem(false);

        //封装成cartvo
        CartVO cartVO = new CartVO();
        cartVO.setCartItems(cartItemVOList);

        return cartVO;
    }


    /**
     * 获取最新的购物项，
     *
     * @param latestPrice 是否获取最新价格
     * @return
     */
    private List<CartItemVO> buildCartItem(boolean latestPrice) {

        BoundHashOperations<String, Object, Object> myCart = getMyCartOps();

        List<Object> itemList = myCart.values();

        List<CartItemVO> cartItemVOList = new ArrayList<>();

        //拼接id列表查询最新价格
        List<Long> productIdList = new ArrayList<>();

        for (Object item : itemList) {
            CartItemVO cartItemVO = JSON.parseObject((String) item, CartItemVO.class);
            cartItemVOList.add(cartItemVO);

            productIdList.add(cartItemVO.getProductId());
        }

        //查询最新的商品价格
        if (latestPrice) {

            setProductLatestPrice(cartItemVOList, productIdList);
        }

        return cartItemVOList;

    }

    /**
     * 设置商品最新价格
     */
    private void setProductLatestPrice(List<CartItemVO> cartItemVOList, List<Long> productIdList) {

        //批量查询
        List<ProductVO> productVOList = productService.findProductsByIdBatch(productIdList);
        //分组
        Map<Long, ProductVO> maps = productVOList.stream().collect(Collectors.toMap(ProductVO::getId, Function.identity()));

        cartItemVOList.parallelStream().forEach(item -> {
            ProductVO productVO = maps.get(item.getProductId());
            item.setProductTitle(productVO.getTitle());
            item.setProductImg(productVO.getCoverImg());
            item.setAmount(productVO.getAmount());
        });


    }


    /**
     * 抽取我的购物车，通用方法
     */
    private BoundHashOperations<String, Object, Object> getMyCartOps() {
        String cartKey = getCartKey();
        return redisTemplate.boundHashOps(cartKey);
    }

    /**
     * 购物车 key
     */
    private String getCartKey() {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        String cartKey = String.format(CacheKey.CART_KEY, loginUser.getId());
        return cartKey;
    }


}
