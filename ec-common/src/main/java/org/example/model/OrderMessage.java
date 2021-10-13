package org.example.model;

import lombok.Data;


@Data
public class OrderMessage {

    /**
     * 消息id
     */
    private Long messageId;

    /**
     * 订单号
     */
    private String outTradeNo;

}
