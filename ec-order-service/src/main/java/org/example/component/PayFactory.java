package org.example.component;

import lombok.extern.slf4j.Slf4j;
import org.example.enums.ProductOrderPayTypeEnum;
import org.example.vo.PayInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class PayFactory {

    @Autowired
    private AlipayStrategy alipayStrategy;

    @Autowired
    private WechatPayStrategy wechatPayStrategy;

    /**
     * 创建支付，简单工程模式
     */
    public String pay(PayInfoVO payInfoVO) {
        String payType = payInfoVO.getPayType();
        if (ProductOrderPayTypeEnum.ALIPAY.name().equalsIgnoreCase(payType)) {
            //支付宝支付
            PayStrategyContext payStrategyContext = new PayStrategyContext(alipayStrategy);
            return payStrategyContext.executeUnifiedorder(payInfoVO);
        } else if (ProductOrderPayTypeEnum.WECHAT.name().equalsIgnoreCase(payType)) {
            //微信支付 TODO
            PayStrategyContext payStrategyContext = new PayStrategyContext(wechatPayStrategy);
            return payStrategyContext.executeUnifiedorder(payInfoVO);
        }
        return "";
    }

    /**
     * 查询订单支付状态
     * 支付成功返回非空，其他返回空
     */
    public String queryPaySuccess(PayInfoVO payInfoVO) {
        String payType = payInfoVO.getPayType();
        if (ProductOrderPayTypeEnum.ALIPAY.name().equalsIgnoreCase(payType)) {
            //支付宝支付
            PayStrategyContext payStrategyContext = new PayStrategyContext(alipayStrategy);
            return payStrategyContext.executeQueryPaySuccess(payInfoVO);
        } else if (ProductOrderPayTypeEnum.WECHAT.name().equalsIgnoreCase(payType)) {
            //微信支付 TODO
            PayStrategyContext payStrategyContext = new PayStrategyContext(wechatPayStrategy);
            return payStrategyContext.executeQueryPaySuccess(payInfoVO);
        }
        return "";
    }


}
