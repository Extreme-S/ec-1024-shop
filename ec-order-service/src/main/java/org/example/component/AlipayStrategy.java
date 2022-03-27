package org.example.component;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.config.AlipayConfig;
import org.example.config.PayUrlConfig;
import org.example.enums.BizCodeEnum;
import org.example.enums.ClientType;
import org.example.exception.BizException;
import org.example.vo.PayInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;


@Slf4j
@Service
public class AlipayStrategy implements PayStrategy {

    @Autowired
    private PayUrlConfig payUrlConfig;

    @Override
    public String unifiedorder(PayInfoVO payInfoVO) {
        HashMap<String, String> content = new HashMap<>();
        //商户订单号,64个字符以内、可包含字母、数字、下划线；需保证在商户端不重复
        content.put("out_trade_no", payInfoVO.getOutTradeNo());
        content.put("product_code", "FAST_INSTANT_TRADE_PAY");
        //订单总金额，单位为元，精确到小数点后两位
        content.put("total_amount", payInfoVO.getPayType());
        //商品标题/交易标题/订单标题/订单关键字等。 注意：不可使用特殊字符，如 /，=，&amp; 等。
        content.put("subject", payInfoVO.getTitle());
        //商品描述，可空
        content.put("body", payInfoVO.getDescription());
        double timeout = Math.floor(payInfoVO.getOrderPayTimeoutMills() * 1.0 / (1000 * 60));
        //前端也需要判断订单是否要关闭了， 如果要快要到期则不给二次支付
        if (timeout < 1) {
            throw new BizException(BizCodeEnum.PAY_ORDER_PAY_TIMEOUT);
        }
        // 该笔订单允许的最晚付款时间，逾期将关闭交易。
        // 取值范围：1m～15d。m-分钟，h-小时，d-天，1c-当天（1c-当天的情况下，无论交易何时创建，都在0点关闭）。
        // 该参数数值不接受小数点， 如 1.5h，可转换为 90m。
        content.put("timeout_express", timeout + "m");

        String clientType = payInfoVO.getClientType();
        String form = "";
        try {
            if (clientType.equalsIgnoreCase(ClientType.H5.name())) {
                //H5手机网页支付
                AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
                request.setBizContent(JSON.toJSONString(content));
                request.setNotifyUrl(payUrlConfig.getAlipayCallbackUrl());
                request.setReturnUrl(payUrlConfig.getAlipaySuccessReturnUrl());
                AlipayTradeWapPayResponse alipayResponse = AlipayConfig.getInstance().pageExecute(request);
                log.info("响应日志:alipayResponse={}", alipayResponse);
                if (alipayResponse.isSuccess()) {
                    form = alipayResponse.getBody();
                } else {
                    log.error("支付宝构建H5表单失败:alipayResponse={},payInfo={}", alipayResponse, payInfoVO);
                }
            } else if (clientType.equalsIgnoreCase(ClientType.PC.name())) {
                //PC支付
                AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
                request.setBizContent(JSON.toJSONString(content));
                request.setNotifyUrl(payUrlConfig.getAlipayCallbackUrl());
                request.setReturnUrl(payUrlConfig.getAlipaySuccessReturnUrl());
                AlipayTradePagePayResponse alipayResponse = AlipayConfig.getInstance().pageExecute(request);
                log.info("响应日志:alipayResponse={}", alipayResponse);
                if (alipayResponse.isSuccess()) {
                    form = alipayResponse.getBody();
                } else {
                    log.error("支付宝构建PC表单失败:alipayResponse={},payInfo={}", alipayResponse, payInfoVO);
                }
            }
        } catch (AlipayApiException e) {
            log.error("支付宝构建表单异常:payInfo={},异常={}", payInfoVO, e);
        }
        return form;
    }

    @Override
    public String refund(PayInfoVO payInfoVO) {
        return null;
    }


    /**
     * 查询订单状态
     * 支付成功：返回非空；其他情况：返回空
     * 未支付返回示例：
     * {
     * "alipay_trade_query_response": {
     * "code": "40004",
     * "msg": "Business Failed",
     * "sub_code": "ACQ.TRADE_NOT_EXIST",
     * "sub_msg": "交易不存在",
     * "buyer_pay_amount": "0.00",
     * "invoice_amount": "0.00",
     * "out_trade_no": "adbe8e8f-3b18-4c9e-b736-02c4c2e15eca",
     * "point_amount": "0.00",
     * "receipt_amount": "0.00"
     * },
     * "sign": "xxxxx"
     * }
     * 已支付返回示例：
     * {
     * "alipay_trade_query_response": {
     * "code": "10000",
     * "msg": "Success",
     * "buyer_logon_id": "mqv***@sandbox.com",
     * "buyer_pay_amount": "0.00",
     * "buyer_user_id": "2088102176996700",
     * "buyer_user_type": "PRIVATE",
     * "invoice_amount": "0.00",
     * "out_trade_no": "adbe8e8f-3b18-4c9e-b736-02c4c2e15eca",
     * "point_amount": "0.00",
     * "receipt_amount": "0.00",
     * "send_pay_date": "2020-12-04 17:06:47",
     * "total_amount": "111.99",
     * "trade_no": "2020120422001496700501648498",
     * "trade_status": "TRADE_SUCCESS"
     * },
     * "sign": "xxxx"
     * }
     */
    @Override
    public String queryPaySuccess(PayInfoVO payInfoVO) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        HashMap<String, String> content = new HashMap<>();
        //订单商户号,64位
        content.put("out_trade_no", payInfoVO.getOutTradeNo());
        request.setBizContent(JSON.toJSONString(content));
        AlipayTradeQueryResponse response = null;
        try {
            response = AlipayConfig.getInstance().execute(request);
            log.info("支付宝订单查询响应：{}", response.getBody());
        } catch (AlipayApiException e) {
            log.error("支付宝订单查询异常:{}", e);
        }
        if (response.isSuccess()) {
            log.info("支付宝订单状态查询成功:{}", payInfoVO);
            return response.getTradeStatus();
        } else {
            log.info("支付宝订单状态查询失败:{}", payInfoVO);
            return "";
        }
    }
}
