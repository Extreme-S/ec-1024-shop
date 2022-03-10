package org.example.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.service.ProductOrderService;
import org.example.request.ConfirmOrderRequest;
import org.example.util.JsonData;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class ProductOrderServiceImpl implements ProductOrderService {


    /**
     * * 防重提交
     * * 用户微服务-确认收货地址
     * * 商品微服务-获取最新购物项和价格
     * * 订单验价
     * * 优惠券微服务-获取优惠券
     * * 验证价格
     * * 锁定优惠券
     * * 锁定商品库存
     * * 创建订单对象
     * * 创建子订单对象
     * * 发送延迟消息-用于自动关单
     * * 创建支付信息-对接三方支付
     */
    @Override
    public JsonData confirmOrder(ConfirmOrderRequest orderRequest) {
        return null;
    }
}
