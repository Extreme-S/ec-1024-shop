package org.example.service;

import org.example.model.OrderMessage;
import org.example.request.ConfirmOrderRequest;
import org.example.util.JsonData;


public interface ProductOrderService {

    /**
     * 创建订单
     */
    JsonData confirmOrder(ConfirmOrderRequest orderRequest);

    /**
     * 查询订单状态
     */
    String queryProductOrderState(String outTradeNo);

    /**
     * 队列监听，定时关单
     */
    boolean closeProductOrder(OrderMessage orderMessage);
}
