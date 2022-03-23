package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.config.RabbitMQConfig;
import org.example.enums.BizCodeEnum;
import org.example.enums.ProductOrderStateEnum;
import org.example.enums.StockTaskStateEnum;
import org.example.exception.BizException;
import org.example.feign.ProductOrderFeignService;
import org.example.mapper.ProductMapper;
import org.example.mapper.ProductTaskMapper;
import org.example.model.ProductDO;
import org.example.model.ProductMessage;
import org.example.model.ProductTaskDO;
import org.example.request.LockProductRequest;
import org.example.request.OrderItemRequest;
import org.example.service.ProductService;
import org.example.util.JsonData;
import org.example.vo.ProductVO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductTaskMapper productTaskMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitMQConfig rabbitMQConfig;

    @Autowired
    private ProductOrderFeignService orderFeignService;

    /**
     * 商品分页
     */
    @Override
    public Map<String, Object> page(int page, int size) {
        Page<ProductDO> pageInfo = new Page<>(page, size);
        IPage<ProductDO> productDOIPage = productMapper.selectPage(pageInfo, null);

        Map<String, Object> pageMap = new HashMap<>(3);
        pageMap.put("total_record", productDOIPage.getTotal());
        pageMap.put("total_page", productDOIPage.getPages());
        pageMap.put("current_data", productDOIPage.getRecords().stream()
                .map(this::beanProcess).collect(Collectors.toList()));
        return pageMap;
    }

    /**
     * 根据id找商品详情
     */
    @Override
    public ProductVO findDetailById(long productId) {
        ProductDO productDO = productMapper.selectById(productId);
        return beanProcess(productDO);
    }

    /**
     * 批量查询
     */
    @Override
    public List<ProductVO> findProductsByIdBatch(List<Long> productIdList) {
        List<ProductDO> productDOList = productMapper.selectList(new QueryWrapper<ProductDO>()
                .in("id", productIdList));
        return productDOList.stream().map(this::beanProcess).collect(Collectors.toList());
    }

    /**
     * 锁定商品库存
     * 1)遍历商品，锁定每个商品购买数量
     * 2)每一次锁定的时候，都要发送延迟消息
     */
    @Override
    public JsonData lockProductStock(LockProductRequest lockProductRequest) {
        String outTradeNo = lockProductRequest.getOrderOutTradeNo();
        List<OrderItemRequest> itemList = lockProductRequest.getOrderItemList();
        //一行代码，提取对象里面的id并加入到集合里面
        List<Long> productIdList = itemList.stream().map(OrderItemRequest::getProductId).collect(Collectors.toList());
        //批量查询
        List<ProductVO> productVOList = this.findProductsByIdBatch(productIdList);
        //分组
        Map<Long, ProductVO> productMap = productVOList.stream().collect(Collectors.toMap(ProductVO::getId, Function.identity()));
        for (OrderItemRequest item : itemList) {
            //锁定商品记录
            int rows = productMapper.lockProductStock(item.getProductId(), item.getBuyNum());
            if (rows != 1) {
                throw new BizException(BizCodeEnum.ORDER_CONFIRM_LOCK_PRODUCT_FAIL);
            }
            //插入商品product_task
            ProductVO productVO = productMap.get(item.getProductId());
            ProductTaskDO productTaskDO = new ProductTaskDO();
            productTaskDO.setBuyNum(item.getBuyNum());
            productTaskDO.setLockState(StockTaskStateEnum.LOCK.name());
            productTaskDO.setProductId(item.getProductId());
            productTaskDO.setProductName(productVO.getTitle());
            productTaskDO.setOutTradeNo(outTradeNo);
            productTaskMapper.insert(productTaskDO);
            log.info("商品库存锁定-插入商品product_task成功:{}", productTaskDO);
            // 发送MQ延迟消息，介绍商品库存
            ProductMessage productMessage = new ProductMessage();
            productMessage.setOutTradeNo(outTradeNo);
            productMessage.setTaskId(productTaskDO.getId());
            rabbitTemplate.convertAndSend(rabbitMQConfig.getEventExchange(), rabbitMQConfig.getStockReleaseDelayRoutingKey(), productMessage);
            log.info("商品库存锁定信息延迟消息发送成功:{}", productMessage);
        }
        return JsonData.buildSuccess();
    }


    /**
     * 释放商品库存
     */
    @Override
    public boolean releaseProductStock(ProductMessage productMessage) {
        //查询工作单状态
        ProductTaskDO productTaskDO = productTaskMapper.selectOne(new QueryWrapper<ProductTaskDO>().eq("id", productMessage.getTaskId()));
        if (productTaskDO == null) {
            log.warn("工作单不存在，消息体为:{}", productMessage);
        }
        //product_task为lock状态才处理
        if (productTaskDO.getLockState().equalsIgnoreCase(StockTaskStateEnum.LOCK.name())) {
            //查询订单状态
            JsonData jsonData = orderFeignService.queryProductOrderState(productMessage.getOutTradeNo());
            if (jsonData.getCode() == 0) {
                String state = jsonData.getData().toString();
                if (ProductOrderStateEnum.NEW.name().equalsIgnoreCase(state)) {
                    //订单状态是NEW新建，则返回给消息队列重新投递
                    log.warn("订单状态是NEW，返回给消息队列，重新投递:{}", productMessage);
                    return false;
                }
                if (ProductOrderStateEnum.PAY.name().equalsIgnoreCase(state)) {
                    //如果已经支付，修改task状态为finish
                    productTaskDO.setLockState(StockTaskStateEnum.FINISH.name());
                    productTaskMapper.update(productTaskDO, new QueryWrapper<ProductTaskDO>().eq("id", productMessage.getTaskId()));
                    log.info("订单已经支付，修改库存锁定工作单FINISH状态:{}", productMessage);
                    return true;
                }
            }
            //订单不存在，或者订单被取消，确认消息,修改task状态为CANCEL
            log.warn("订单不存在,或订单被取消，确认消息，修改task状态为CANCEL,恢复商品库存,message:{}", productMessage);
            productTaskDO.setLockState(StockTaskStateEnum.CANCEL.name());
            productTaskMapper.update(productTaskDO, new QueryWrapper<ProductTaskDO>().eq("id", productMessage.getTaskId()));
            //恢复商品库存，集锁定库存的值减去当前购买的值
            productMapper.unlockProductStock(productTaskDO.getProductId(), productTaskDO.getBuyNum());
        } else {
            log.warn("工作单状态不是LOCK,state={},消息体={}", productTaskDO.getLockState(), productMessage);
        }
        return true;
    }

    private ProductVO beanProcess(ProductDO productDO) {
        ProductVO productVO = new ProductVO();
        BeanUtils.copyProperties(productDO, productVO);
        productVO.setStock(productDO.getStock() - productDO.getLockStock());
        return productVO;
    }
}
