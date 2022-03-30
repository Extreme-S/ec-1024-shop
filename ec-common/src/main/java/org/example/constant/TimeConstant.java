package org.example.constant;


public class TimeConstant {

    /**
     * 支付订单的有效时长/毫秒，超过未支付则关闭订单
     * 默认30min，测试5min
     */
    //public static final long ORDER_PAY_TIMEOUT_MILLS = 30*60*1000;
    public static final long ORDER_PAY_TIMEOUT_MILLS = 5 * 60 * 1000;

}
