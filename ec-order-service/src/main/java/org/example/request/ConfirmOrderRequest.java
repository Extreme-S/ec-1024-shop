package org.example.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ConfirmOrderRequest {

    /**
     * 购物车使用的优惠券，集满减劵
     * 注意：如果传空或者小于0,则不用优惠券
     */
    @JsonProperty("coupon_record_id")
    private Long couponRecordId;

    /**
     * 最终购买的商品列表
     * 传递id，购买数量从购物车中读取
     */
    @JsonProperty("product_ids")
    private List<Long> productIdList;

    /**
     * 支付方式
     */
    @JsonProperty("pay_type")
    private String payType;

    /**
     * 端类型
     */
    @JsonProperty("client_type")
    private String clientType;

    /**
     * 收货地址id
     */
    @JsonProperty("address_id")
    private long addressId;

    /**
     * 总价格，前端传递，后端需要验价
     */
    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    /**
     * 实际支付的价格，
     * 如果用了优惠劵，则是减去优惠券后端价格，如果没的话，则是totalAmount一样
     */
    @JsonProperty("real_pay_amount")
    private BigDecimal realPayAmount;

    /**
     * 防重令牌
     */
    @JsonProperty("token")
    private String token;

}
