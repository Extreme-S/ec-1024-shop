package org.example.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayInfoVO {

    /**
     * 订单号
     */
    private String outTradeNo;

    /**
     * 订单总金额
     */
    private BigDecimal payFee;

    /**
     * 支付类型 微信-支付宝-银行-其他
     */
    private String payType;

    /**
     * 端类型 APP/H5/PC
     */
    private String clientType;

    /**
     * 标题
     */
    private String title;

    /**
     * 描述
     */
    private String description;
    
    /**
     * 订单支付超时时间，毫秒
     */
    private long orderPayTimeoutMills;

}
