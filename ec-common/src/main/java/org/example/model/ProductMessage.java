package org.example.model;

import lombok.Data;


@Data
public class ProductMessage {


    /**
     * 消息队列id
     */
    private long messageId;

    /**
     * 订单号
     */
    private String outTradeNo;

    /**
     * 库存锁定taskId
     */
    private long taskId;
}
