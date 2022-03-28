package org.example.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.example.config.AlipayConfig;
import org.example.enums.ProductOrderPayTypeEnum;
import org.example.service.ProductOrderService;
import org.example.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@Api("订单回调通知模块")
@Controller
@RequestMapping("/api/callback/order/v1")
@Slf4j
public class CallbackController {

    @Autowired
    private ProductOrderService productOrderService;

    /**
     * 支付回调通知 post方式
     */
    @PostMapping("alipay")
    public String alipayCallback(HttpServletRequest request, HttpServletResponse response) {
        //将异步通知中收到的所有参数存储到map中
        Map<String, String> paramsMap = convertRequestParamsToMap(request);
        log.info("支付宝回调通知结果:{}", paramsMap);
        //调用SDK验证签名
        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(paramsMap, AlipayConfig.ALIPAY_PUB_KEY, AlipayConfig.CHARSET, AlipayConfig.SIGN_TYPE);
            if (signVerified) {
                JsonData jsonData = productOrderService.handlerOrderCallbackMsg(ProductOrderPayTypeEnum.ALIPAY, paramsMap);
                if (jsonData.getCode() == 0) {
                    //通知结果确认成功，不然会一直通知，八次都没返回success就认为交易失败
                    return "success";
                }
            }
        } catch (AlipayApiException e) {
            log.info("支付宝回调验证签名失败:异常：{}，参数:{}", e, paramsMap);
        }
        return "failure";
    }


    /**
     * 将request中的参数转换成Map
     */
    private static Map<String, String> convertRequestParamsToMap(HttpServletRequest request) {
        Map<String, String> paramsMap = new HashMap<>(16);
        Set<Map.Entry<String, String[]>> entrySet = request.getParameterMap().entrySet();
        for (Map.Entry<String, String[]> entry : entrySet) {
            String name = entry.getKey();
            String[] values = entry.getValue();
            int size = values.length;
            if (size == 1) {
                paramsMap.put(name, values[0]);
            } else {
                paramsMap.put(name, "");
            }
        }
        return paramsMap;
    }


}
