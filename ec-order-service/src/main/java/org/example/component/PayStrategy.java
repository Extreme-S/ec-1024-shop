package org.example.component;

import org.example.vo.PayInfoVO;


public interface PayStrategy {


    /**
     * 下单
     */
    String unifiedorder(PayInfoVO payInfoVO);

    /**
     * 退款
     */
    default String refund(PayInfoVO payInfoVO) {
        return "";
    }

    /**
     * 查询支付是否成功
     */
    default String queryPaySuccess(PayInfoVO payInfoVO) {
        return "";
    }


}
