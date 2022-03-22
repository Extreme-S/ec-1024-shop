package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.config.RabbitMQConfig;
import org.example.enums.BizCodeEnum;
import org.example.enums.CouponStateEnum;
import org.example.enums.ProductOrderStateEnum;
import org.example.enums.StockTaskStateEnum;
import org.example.exception.BizException;
import org.example.feign.ProductOrderFeignService;
import org.example.interceptor.LoginInterceptor;
import org.example.mapper.CouponRecordMapper;
import org.example.mapper.CouponTaskMapper;
import org.example.model.CouponRecordDO;
import org.example.model.CouponRecordMessage;
import org.example.model.CouponTaskDO;
import org.example.model.LoginUser;
import org.example.request.LockCouponRecordRequest;
import org.example.service.CouponRecordService;
import org.example.util.JsonData;
import org.example.vo.CouponRecordVO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Slf4j
public class CouponRecordServiceImpl implements CouponRecordService {

    @Autowired
    private CouponRecordMapper couponRecordMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitMQConfig rabbitMQConfig;

    @Autowired
    private CouponTaskMapper couponTaskMapper;

    @Autowired
    private ProductOrderFeignService orderFeignService;

    @Override
    public Map<String, Object> page(int page, int size) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        //封装分页信息
        Page<CouponRecordDO> pageInfo = new Page<>(page, size);
        IPage<CouponRecordDO> recordDOIPage = couponRecordMapper.selectPage(pageInfo, new QueryWrapper<CouponRecordDO>()
                .eq("user_id", loginUser.getId())
                .orderByDesc("create_time"));
        HashMap<String, Object> pageMap = new HashMap<>(3);
        pageMap.put("total_record", recordDOIPage.getTotal());
        pageMap.put("total_page", recordDOIPage.getPages());
        pageMap.put("current_data", recordDOIPage.getRecords().stream()
                .map(this::beanProcess).collect(Collectors.toList()));
        return pageMap;
    }

    @Override
    public CouponRecordVO findById(long recordId) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        CouponRecordDO couponRecordDO = couponRecordMapper.selectOne(new QueryWrapper<CouponRecordDO>()
                .eq("id", recordId)
                .eq("user_id", loginUser.getId()));
        if (couponRecordDO == null) {
            return null;
        }
        return beanProcess(couponRecordDO);
    }

    /**
     * 锁定优惠券
     * 1）锁定优惠券记录
     * 2）task表插入记录
     * 3）发送延迟消息
     */
    @Override
    public JsonData lockCouponRecords(LockCouponRecordRequest recordRequest) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        String orderOutTradeNo = recordRequest.getOrderOutTradeNo();
        List<Long> lockCouponRecordIds = recordRequest.getLockCouponRecordIds();
        int updateRows = couponRecordMapper.lockUseStateBatch(
                loginUser.getId(), CouponStateEnum.USED.name(), lockCouponRecordIds);
        //生成 couponTaskDOList
        List<CouponTaskDO> couponTaskDOList = lockCouponRecordIds.stream().map(obj -> {
            CouponTaskDO couponTaskDO = new CouponTaskDO();
            couponTaskDO.setCreateTime(new Date());
            couponTaskDO.setOutTradeNo(orderOutTradeNo);
            couponTaskDO.setCouponRecordId(obj);
            couponTaskDO.setLockState(StockTaskStateEnum.LOCK.name());
            return couponTaskDO;
        }).collect(Collectors.toList());
        int insertRows = couponTaskMapper.insertBatch(couponTaskDOList);
        log.info("优惠券记录锁定updateRows={}", updateRows);
        log.info("新增优惠券记录task insertRows={}", insertRows);
        if (lockCouponRecordIds.size() == insertRows && insertRows == updateRows) {
            //发送延迟消息
            for (CouponTaskDO couponTaskDO : couponTaskDOList) {
                CouponRecordMessage couponRecordMessage = new CouponRecordMessage();
                couponRecordMessage.setOutTradeNo(orderOutTradeNo);
                couponRecordMessage.setTaskId(couponTaskDO.getId());
                //投递消息
                rabbitTemplate.convertAndSend(rabbitMQConfig.getEventExchange(),
                        rabbitMQConfig.getCouponReleaseDelayRoutingKey(), couponRecordMessage);
                log.info("优惠券锁定消息发送成功:{}", couponRecordMessage.toString());
            }
            return JsonData.buildSuccess();
        } else {
            throw new BizException(BizCodeEnum.COUPON_RECORD_LOCK_FAIL);
        }
    }

    /**
     * 解锁优惠券记录
     * 1）查询task工作单是否存在
     * 2) 查询订单状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public boolean releaseCouponRecord(CouponRecordMessage recordMessage) {
        //查询couponTask表中对应taskId的couponTask对象
        CouponTaskDO taskDO = couponTaskMapper.selectOne(
                new QueryWrapper<CouponTaskDO>().eq("id", recordMessage.getTaskId()));
        if (taskDO == null) {
            log.warn("工作单不存，消息:{}", recordMessage);
            return true;
        }

        //coupon_task为lock状态才处理
        if (taskDO.getLockState().equalsIgnoreCase(StockTaskStateEnum.LOCK.name())) {
            //查询订单状态
            JsonData jsonData = orderFeignService.queryProductOrderState(recordMessage.getOutTradeNo());
            if (jsonData.getCode() == 0) {
                //正常响应，判断订单状态
                String orderState = jsonData.getData().toString();
                if (ProductOrderStateEnum.NEW.name().equalsIgnoreCase(orderState)) {
                    //订单状态为NEW新建，则返回给消息队列重新投递
                    log.warn("订单状态是NEW，返回给消息队列，重新投递:{}", recordMessage);
                    return false;
                }
                if (ProductOrderStateEnum.PAY.name().equalsIgnoreCase(orderState)) {
                    //订单状态为PAY已支付，修改coupon_task状态为FINISH
                    taskDO.setLockState(StockTaskStateEnum.FINISH.name());
                    couponTaskMapper.update(taskDO, new QueryWrapper<CouponTaskDO>().eq("id", recordMessage.getTaskId()));
                    log.info("订单已经支付，修改库存锁定工作单FINISH状态:{}", recordMessage);
                    return true;
                }
            }
            //订单不存在，或者订单被取消，确认消息,修改task状态为CANCEL,恢复优惠券使用记录为NEW
            log.warn("订单不存在，或者订单被取消，确认消息,修改task状态为CANCEL,恢复优惠券使用记录为NEW,message:{}", recordMessage);
            taskDO.setLockState(StockTaskStateEnum.CANCEL.name());
            couponTaskMapper.update(taskDO, new QueryWrapper<CouponTaskDO>().eq("id", recordMessage.getTaskId()));
            //恢复优惠券记录为NEW状态
            couponRecordMapper.updateState(taskDO.getCouponRecordId(), CouponStateEnum.NEW.name());
        } else {
            log.warn("工作单状态不是LOCK,state={},消息体={}", taskDO.getLockState(), recordMessage);
        }
        return true;
    }


    private CouponRecordVO beanProcess(CouponRecordDO couponRecordDO) {
        CouponRecordVO couponRecordVO = new CouponRecordVO();
        BeanUtils.copyProperties(couponRecordDO, couponRecordVO);
        return couponRecordVO;
    }
}
