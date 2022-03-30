package org.example.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.component.PayFactory;
import org.example.config.RabbitMQConfig;
import org.example.constant.CacheKey;
import org.example.constant.TimeConstant;
import org.example.enums.*;
import org.example.exception.BizException;
import org.example.feign.CouponFeignService;
import org.example.feign.ProductFeignService;
import org.example.feign.UserFeignService;
import org.example.interceptor.LoginInterceptor;
import org.example.mapper.ProductOrderItemMapper;
import org.example.mapper.ProductOrderMapper;
import org.example.model.LoginUser;
import org.example.model.OrderMessage;
import org.example.model.ProductOrderDO;
import org.example.model.ProductOrderItemDO;
import org.example.request.*;
import org.example.service.ProductOrderService;
import org.example.util.CommonUtil;
import org.example.util.JsonData;
import org.example.vo.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ProductOrderServiceImpl implements ProductOrderService {

    @Autowired
    private ProductOrderMapper productOrderMapper;

    @Autowired
    private ProductOrderItemMapper orderItemMapper;

    @Autowired
    private UserFeignService userFeignService;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitMQConfig rabbitMQConfig;

    @Autowired
    private PayFactory payFactory;

    @Autowired
    private StringRedisTemplate redisTemplate;


    /**
     * * 防重提交
     * * 用户微服务-确认收货地址
     * * 商品微服务-获取最新购物项和价格
     * * 订单验价
     * * 优惠券微服务-获取优惠券
     * * 验证价格
     * * 锁定优惠券
     * * 锁定商品库存
     * * 创建订单对象
     * * 创建子订单对象
     * * 发送延迟消息-用于自动关单
     * * 创建支付信息-对接三方支付
     */
    @Override
    public JsonData confirmOrder(ConfirmOrderRequest orderRequest) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        //校验支付令牌
        String orderToken = orderRequest.getToken();
        if (StringUtils.isBlank(orderToken)) {
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_TOKEN_NOT_EXIST);
        }
        //lua脚本， 原子操作 校验令牌，删除令牌
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        Long result = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                List.of(String.format(CacheKey.SUBMIT_ORDER_TOKEN_KEY, loginUser.getId())), orderToken);
        if (result == 0L) {
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_TOKEN_EQUAL_FAIL);
        }

        String orderOutTradeNo = CommonUtil.getStringNumRandom(32);
        //获取收货地址详情
        ProductOrderAddressVO addressVO = this.getUserAddress(orderRequest.getAddressId());
        log.info("收货地址信息:{}", addressVO);
        //获取用户加入购物车的商品
        List<Long> productIdList = orderRequest.getProductIdList();
        JsonData cartItemData = productFeignService.confirmOrderCartItem(productIdList);
        List<OrderItemVO> orderItemList = cartItemData.getData(new TypeReference<>() {
        });
        log.info("获取的商品:{}", orderItemList);
        if (orderItemList == null) {
            //购物车商品不存在
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_CART_ITEM_NOT_EXIST);
        }
        //验证价格，减去商品优惠券
        this.checkPrice(orderItemList, orderRequest);
        //锁定优惠券
        this.lockCouponRecords(orderRequest, orderOutTradeNo);
        //锁定库存
        this.lockProductStocks(orderItemList, orderOutTradeNo);
        //创建订单
        ProductOrderDO productOrderDO = this.saveProductOrder(orderRequest, loginUser, orderOutTradeNo, addressVO);
        //创建订单项
        this.saveProductOrderItems(orderOutTradeNo, productOrderDO.getId(), orderItemList);
        //发送延迟消息，用于自动关单 TODO
        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setOutTradeNo(orderOutTradeNo);
        rabbitTemplate.convertAndSend(rabbitMQConfig.getEventExchange(), rabbitMQConfig.getOrderCloseDelayRoutingKey(), orderMessage);

        //创建支付
        PayInfoVO payInfoVO = new PayInfoVO(orderOutTradeNo, productOrderDO.getPayAmount(), orderRequest.getPayType(),
                orderRequest.getClientType(), orderItemList.get(0).getProductTitle(), "",
                TimeConstant.ORDER_PAY_TIMEOUT_MILLS);
        String payResult = payFactory.pay(payInfoVO);
        if (StringUtils.isNotBlank(payResult)) {
            log.info("创建支付订单成功:payInfoVO={},payResult={}", payInfoVO, payResult);
            return JsonData.buildSuccess(payResult);
        } else {
            log.error("创建支付订单失败:payInfoVO={},payResult={}", payInfoVO, payResult);
            return JsonData.buildResult(BizCodeEnum.PAY_ORDER_FAIL);
        }
    }

    /**
     * 新增订单项
     */
    private void saveProductOrderItems(String orderOutTradeNo, Long orderId, List<OrderItemVO> orderItemList) {
        List<ProductOrderItemDO> list = orderItemList.stream().map(
                obj -> {
                    ProductOrderItemDO itemDO = new ProductOrderItemDO();
                    itemDO.setBuyNum(obj.getBuyNum());
                    itemDO.setProductId(obj.getProductId());
                    itemDO.setProductImg(obj.getProductImg());
                    itemDO.setProductName(obj.getProductTitle());
                    itemDO.setOutTradeNo(orderOutTradeNo);
                    itemDO.setCreateTime(new Date());
                    //单价
                    itemDO.setAmount(obj.getAmount());
                    //总价
                    itemDO.setTotalAmount(obj.getTotalAmount());
                    itemDO.setProductOrderId(orderId);
                    return itemDO;
                }
        ).collect(Collectors.toList());
        orderItemMapper.insertBatch(list);
    }

    /**
     * 创建订单
     */
    private ProductOrderDO saveProductOrder(ConfirmOrderRequest orderRequest, LoginUser loginUser, String orderOutTradeNo, ProductOrderAddressVO addressVO) {
        ProductOrderDO productOrderDO = new ProductOrderDO();
        productOrderDO.setUserId(loginUser.getId());
        productOrderDO.setHeadImg(loginUser.getHeadImg());
        productOrderDO.setNickname(loginUser.getName());
        productOrderDO.setOutTradeNo(orderOutTradeNo);
        productOrderDO.setCreateTime(new Date());
        productOrderDO.setDel(0);
        productOrderDO.setOrderType(ProductOrderTypeEnum.DAILY.name());
        //实际支付的价格
        productOrderDO.setPayAmount(orderRequest.getRealPayAmount());
        //总价，未使用优惠券的价格
        productOrderDO.setTotalAmount(orderRequest.getTotalAmount());
        productOrderDO.setState(ProductOrderStateEnum.NEW.name());
        productOrderDO.setOrderType(ProductOrderTypeEnum.DAILY.name());
        productOrderDO.setPayType(ProductOrderPayTypeEnum.valueOf(orderRequest.getPayType()).name());
        productOrderDO.setReceiverAddress(JSON.toJSONString(addressVO));
        productOrderMapper.insert(productOrderDO);
        return productOrderDO;
    }


    /**
     * 锁定商品库存
     */
    private void lockProductStocks(List<OrderItemVO> orderItemList, String orderOutTradeNo) {
        List<OrderItemRequest> itemRequestList = orderItemList.stream().map(obj -> {
            OrderItemRequest request = new OrderItemRequest();
            request.setBuyNum(obj.getBuyNum());
            request.setProductId(obj.getProductId());
            return request;
        }).collect(Collectors.toList());
        LockProductRequest lockProductRequest = new LockProductRequest();
        lockProductRequest.setOrderOutTradeNo(orderOutTradeNo);
        lockProductRequest.setOrderItemList(itemRequestList);
        JsonData jsonData = productFeignService.lockProductStock(lockProductRequest);
        if (jsonData.getCode() != 0) {
            log.error("锁定商品库存失败：{}", lockProductRequest);
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_LOCK_PRODUCT_FAIL);
        }
    }

    /**
     * 锁定优惠券
     */
    private void lockCouponRecords(ConfirmOrderRequest orderRequest, String orderOutTradeNo) {
        List<Long> lockCouponRecordIds = new ArrayList<>();
        if (orderRequest.getCouponRecordId() > 0) {
            lockCouponRecordIds.add(orderRequest.getCouponRecordId());
            LockCouponRecordRequest lockCouponRecordRequest = new LockCouponRecordRequest();
            lockCouponRecordRequest.setOrderOutTradeNo(orderOutTradeNo);
            lockCouponRecordRequest.setLockCouponRecordIds(lockCouponRecordIds);
            //发起锁定优惠券请求
            JsonData jsonData = couponFeignService.lockCouponRecords(lockCouponRecordRequest);
            if (jsonData.getCode() != 0) {
                throw new BizException(BizCodeEnum.COUPON_RECORD_LOCK_FAIL);
            }
        }
    }

    /**
     * 验证价格
     * 1）统计全部商品的价格
     * 2) 获取优惠券(判断是否满足优惠券的条件)，总价再减去优惠券的价格 就是 最终的价格
     */
    private void checkPrice(List<OrderItemVO> orderItemList, ConfirmOrderRequest orderRequest) {
        //统计商品总价格
        BigDecimal realPayAmount = new BigDecimal("0");
        if (orderItemList != null) {
            for (OrderItemVO orderItemVO : orderItemList) {
                BigDecimal itemRealPayAmount = orderItemVO.getTotalAmount();
                realPayAmount = realPayAmount.add(itemRealPayAmount);
            }
        }
        //获取优惠券，判断是否可以使用
        CouponRecordVO couponRecordVO = getCartCouponRecord(orderRequest.getCouponRecordId());
        //计算购物车价格，是否满足优惠券满减条件
        if (couponRecordVO != null) {
            //计算是否满足满减
            if (realPayAmount.compareTo(couponRecordVO.getConditionPrice()) < 0) {
                throw new BizException(BizCodeEnum.ORDER_CONFIRM_COUPON_FAIL);
            }
            if (couponRecordVO.getPrice().compareTo(realPayAmount) > 0) {
                realPayAmount = BigDecimal.ZERO;
            } else {
                realPayAmount = realPayAmount.subtract(couponRecordVO.getPrice());
            }
        }
        if (realPayAmount.compareTo(orderRequest.getRealPayAmount()) != 0) {
            log.error("订单验价失败：{}", orderRequest);
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_PRICE_FAIL);
        }
    }

    /**
     * 获取优惠券
     */
    private CouponRecordVO getCartCouponRecord(Long couponRecordId) {
        if (couponRecordId == null || couponRecordId < 0) {
            return null;
        }
        JsonData couponData = couponFeignService.findUserCouponRecordById(couponRecordId);
        if (couponData.getCode() != 0) {
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_COUPON_FAIL);
        }
        if (couponData.getCode() == 0) {
            CouponRecordVO couponRecordVO = couponData.getData(new TypeReference<>() {
            });
            if (!couponAvailable(couponRecordVO)) {
                log.error("优惠券使用失败");
                throw new BizException(BizCodeEnum.COUPON_UNAVAILABLE);
            }
            return couponRecordVO;
        }
        return null;
    }

    /**
     * 判断优惠券是否可用
     */
    private boolean couponAvailable(CouponRecordVO couponRecordVO) {
        if (couponRecordVO.getUseState().equalsIgnoreCase(CouponStateEnum.NEW.name())) {
            long currentTimestamp = CommonUtil.getCurrentTimestamp();
            long end = couponRecordVO.getEndTime().getTime();
            long start = couponRecordVO.getStartTime().getTime();
            return currentTimestamp >= start && currentTimestamp <= end;
        }
        return false;
    }

    /**
     * 获取收货地址详情
     */
    private ProductOrderAddressVO getUserAddress(long addressId) {
        JsonData addressData = userFeignService.detail(addressId);
        if (addressData.getCode() != 0) {
            log.error("获取收获地址失败,msg:{}", addressData);
            throw new BizException(BizCodeEnum.ADDRESS_NO_EXITS);
        }
        return addressData.getData(new TypeReference<>() {
        });
    }

    /**
     * 查询订单状态
     */
    @Override
    public String queryProductOrderState(String outTradeNo) {
        ProductOrderDO productOrderDO = productOrderMapper.selectOne(
                new QueryWrapper<ProductOrderDO>().eq("out_trade_no", outTradeNo));
        return productOrderDO == null ? "" : productOrderDO.getState();
    }

    /**
     * 定时关单
     */
    @Override
    public boolean closeProductOrder(OrderMessage orderMessage) {
        //查询订单数据库，判断订单是否支付
        ProductOrderDO productOrderDO = productOrderMapper.selectOne(
                new QueryWrapper<ProductOrderDO>().eq("out_trade_no", orderMessage.getOutTradeNo()));
        if (productOrderDO == null) {
            //订单不存在
            log.warn("直接确认消息，订单不存在:{}", orderMessage);
            return true;
        }
        if (productOrderDO.getState().equalsIgnoreCase(ProductOrderStateEnum.PAY.name())) {
            //已经支付
            log.info("直接确认消息,订单已经支付:{}", orderMessage);
            return true;
        }
        //如果是未支付，向第三方支付查询订单是否真的未支付
        PayInfoVO payInfoVO = new PayInfoVO();
        payInfoVO.setPayType(productOrderDO.getPayType());
        payInfoVO.setOutTradeNo(orderMessage.getOutTradeNo());
        String payResult = payFactory.queryPaySuccess(payInfoVO);

        //结果为空，则未支付成功，本地取消订单
        if (StringUtils.isBlank(payResult)) {
            productOrderMapper.updateOrderPayState(productOrderDO.getOutTradeNo(), ProductOrderStateEnum.CANCEL.name(), ProductOrderStateEnum.NEW.name());
            log.info("结果为空，则未支付成功，本地取消订单:{}", orderMessage);
            return true;
        } else {
            //支付成功，主动的把订单状态改成UI就支付，造成该原因的情况可能是支付通道回调有问题
            log.warn("支付成功，主动的把订单状态改成UI就支付，造成该原因的情况可能是支付通道回调有问题:{}", orderMessage);
            productOrderMapper.updateOrderPayState(productOrderDO.getOutTradeNo(), ProductOrderStateEnum.PAY.name(), ProductOrderStateEnum.NEW.name());
            return true;
        }
    }

    /***
     * 支付通知结果更新订单状态
     */
    @Override
    public JsonData handlerOrderCallbackMsg(ProductOrderPayTypeEnum payType, Map<String, String> paramsMap) {
        if (payType.name().equalsIgnoreCase(ProductOrderPayTypeEnum.ALIPAY.name())) {
            //支付宝支付
            //获取商户订单号
            String outTradeNo = paramsMap.get("out_trade_no");
            //交易的状态
            String tradeStatus = paramsMap.get("trade_status");
            if ("TRADE_SUCCESS".equalsIgnoreCase(tradeStatus) || "TRADE_FINISHED".equalsIgnoreCase(tradeStatus)) {
                //更新订单状态
                productOrderMapper.updateOrderPayState(outTradeNo, ProductOrderStateEnum.PAY.name(), ProductOrderStateEnum.NEW.name());
                return JsonData.buildSuccess();
            }
        } else if (payType.name().equalsIgnoreCase(ProductOrderPayTypeEnum.WECHAT.name())) {
            //微信支付  TODO
        }
        return JsonData.buildResult(BizCodeEnum.PAY_ORDER_CALLBACK_NOT_SUCCESS);
    }

    /**
     * 分页查询我的订单
     */
    @Override
    public Map<String, Object> page(int page, int size, String state) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        Page<ProductOrderDO> pageInfo = new Page<>(page, size);
        IPage<ProductOrderDO> orderDOPage = null;
        if (StringUtils.isBlank(state)) {
            orderDOPage = productOrderMapper.selectPage(pageInfo, new QueryWrapper<ProductOrderDO>().eq("user_id", loginUser.getId()));
        } else {
            orderDOPage = productOrderMapper.selectPage(pageInfo, new QueryWrapper<ProductOrderDO>().eq("user_id", loginUser.getId()).eq("state", state));
        }
        //获取订单列表
        List<ProductOrderDO> productOrderDOList = orderDOPage.getRecords();
        List<ProductOrderVO> productOrderVOList = productOrderDOList.stream().map(orderDO -> {
            List<ProductOrderItemDO> itemDOList = orderItemMapper.selectList(new QueryWrapper<ProductOrderItemDO>()
                    .eq("product_order_id", orderDO.getId()));
            List<OrderItemVO> itemVOList = itemDOList.stream().map(item -> {
                OrderItemVO itemVO = new OrderItemVO();
                BeanUtils.copyProperties(item, itemVO);
                return itemVO;
            }).collect(Collectors.toList());
            ProductOrderVO productOrderVO = new ProductOrderVO();
            BeanUtils.copyProperties(orderDO, productOrderVO);
            productOrderVO.setOrderItemList(itemVOList);
            return productOrderVO;
        }).collect(Collectors.toList());
        Map<String, Object> pageMap = new HashMap<>(3);
        pageMap.put("total_record", orderDOPage.getTotal());
        pageMap.put("total_page", orderDOPage.getPages());
        pageMap.put("current_data", productOrderVOList);
        return pageMap;
    }

    /**
     * 订单二次支付
     */
    @Override
    @Transactional
    public JsonData repay(RepayOrderRequest repayOrderRequest) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        ProductOrderDO productOrderDO = productOrderMapper.selectOne(new QueryWrapper<ProductOrderDO>()
                .eq("out_trade_no", repayOrderRequest.getOutTradeNo())
                .eq("user_id", loginUser.getId()));
        log.info("订单状态:{}", productOrderDO);
        if (productOrderDO == null) {
            return JsonData.buildResult(BizCodeEnum.PAY_ORDER_NOT_EXIST);
        }
        //订单状态不对，不是NEW状态
        if (!productOrderDO.getState().equalsIgnoreCase(ProductOrderStateEnum.NEW.name())) {
            return JsonData.buildResult(BizCodeEnum.PAY_ORDER_STATE_ERROR);
        } else {
            //订单创建到现在的存活时间
            long orderLiveTime = CommonUtil.getCurrentTimestamp() - productOrderDO.getCreateTime().getTime();
            //创建订单是临界点，所以再增加1分钟多几秒，假如29分，则也不能支付了
            orderLiveTime = orderLiveTime + 70 * 1000;
            //大于订单超时时间，则失效
            if (orderLiveTime > TimeConstant.ORDER_PAY_TIMEOUT_MILLS) {
                return JsonData.buildResult(BizCodeEnum.PAY_ORDER_PAY_TIMEOUT);
            } else {
                //更新DB订单支付参数payType，还可以增加订单支付信息日志 TODO
                //总时间-存活的时间 = 剩下的有效时间
                long timeout = TimeConstant.ORDER_PAY_TIMEOUT_MILLS - orderLiveTime;
                //创建支付
                PayInfoVO payInfoVO = new PayInfoVO(productOrderDO.getOutTradeNo(), productOrderDO.getPayAmount(),
                        repayOrderRequest.getPayType(), repayOrderRequest.getClientType(),
                        productOrderDO.getOutTradeNo(), "", timeout);
                log.info("payInfoVO={}", payInfoVO);
                String payResult = payFactory.pay(payInfoVO);
                if (StringUtils.isNotBlank(payResult)) {
                    log.info("创建二次支付订单成功:payInfoVO={},payResult={}", payInfoVO, payResult);
                    return JsonData.buildSuccess(payResult);
                } else {
                    log.error("创建二次支付订单失败:payInfoVO={},payResult={}", payInfoVO, payResult);
                    return JsonData.buildResult(BizCodeEnum.PAY_ORDER_FAIL);
                }
            }
        }
    }
}
