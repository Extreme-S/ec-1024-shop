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
     * ๅๅๅ้กต
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
     * ๆ?นๆฎidๆพๅๅ่ฏฆๆ
     */
    @Override
    public ProductVO findDetailById(long productId) {
        ProductDO productDO = productMapper.selectById(productId);
        return beanProcess(productDO);
    }

    /**
     * ๆน้ๆฅ่ฏข
     */
    @Override
    public List<ProductVO> findProductsByIdBatch(List<Long> productIdList) {
        List<ProductDO> productDOList = productMapper.selectList(new QueryWrapper<ProductDO>()
                .in("id", productIdList));
        return productDOList.stream().map(this::beanProcess).collect(Collectors.toList());
    }

    /**
     * ้ๅฎๅๅๅบๅญ
     * 1)้ๅๅๅ๏ผ้ๅฎๆฏไธชๅๅ่ดญไนฐๆฐ้
     * 2)ๆฏไธๆฌก้ๅฎ็ๆถๅ๏ผ้ฝ่ฆๅ้ๅปถ่ฟๆถๆฏ
     */
    @Override
    public JsonData lockProductStock(LockProductRequest lockProductRequest) {
        String outTradeNo = lockProductRequest.getOrderOutTradeNo();
        List<OrderItemRequest> itemList = lockProductRequest.getOrderItemList();
        //ไธ่กไปฃ็?๏ผๆๅๅฏน่ฑก้้ข็idๅนถๅ?ๅฅๅฐ้ๅ้้ข
        List<Long> productIdList = itemList.stream().map(OrderItemRequest::getProductId).collect(Collectors.toList());
        //ๆน้ๆฅ่ฏข
        List<ProductVO> productVOList = this.findProductsByIdBatch(productIdList);
        //ๅ็ป
        Map<Long, ProductVO> productMap = productVOList.stream().collect(Collectors.toMap(ProductVO::getId, Function.identity()));
        for (OrderItemRequest item : itemList) {
            //้ๅฎๅๅ่ฎฐๅฝ
            int rows = productMapper.lockProductStock(item.getProductId(), item.getBuyNum());
            if (rows != 1) {
                throw new BizException(BizCodeEnum.ORDER_CONFIRM_LOCK_PRODUCT_FAIL);
            }
            //ๆๅฅๅๅproduct_task
            ProductVO productVO = productMap.get(item.getProductId());
            ProductTaskDO productTaskDO = new ProductTaskDO();
            productTaskDO.setBuyNum(item.getBuyNum());
            productTaskDO.setLockState(StockTaskStateEnum.LOCK.name());
            productTaskDO.setProductId(item.getProductId());
            productTaskDO.setProductName(productVO.getTitle());
            productTaskDO.setOutTradeNo(outTradeNo);
            productTaskMapper.insert(productTaskDO);
            log.info("ๅๅๅบๅญ้ๅฎ-ๆๅฅๅๅproduct_taskๆๅ:{}", productTaskDO);
            // ๅ้MQๅปถ่ฟๆถๆฏ๏ผไป็ปๅๅๅบๅญ
            ProductMessage productMessage = new ProductMessage();
            productMessage.setOutTradeNo(outTradeNo);
            productMessage.setTaskId(productTaskDO.getId());
            rabbitTemplate.convertAndSend(rabbitMQConfig.getEventExchange(), rabbitMQConfig.getStockReleaseDelayRoutingKey(), productMessage);
            log.info("ๅๅๅบๅญ้ๅฎไฟกๆฏๅปถ่ฟๆถๆฏๅ้ๆๅ:{}", productMessage);
        }
        return JsonData.buildSuccess();
    }


    /**
     * ้ๆพๅๅๅบๅญ
     */
    @Override
    public boolean releaseProductStock(ProductMessage productMessage) {
        //ๆฅ่ฏขๅทฅไฝๅ็ถๆ
        ProductTaskDO productTaskDO = productTaskMapper.selectOne(new QueryWrapper<ProductTaskDO>().eq("id", productMessage.getTaskId()));
        if (productTaskDO == null) {
            log.warn("ๅทฅไฝๅไธๅญๅจ๏ผๆถๆฏไฝไธบ:{}", productMessage);
        }
        //product_taskไธบlock็ถๆๆๅค็
        if (productTaskDO.getLockState().equalsIgnoreCase(StockTaskStateEnum.LOCK.name())) {
            //ๆฅ่ฏข่ฎขๅ็ถๆ
            JsonData jsonData = orderFeignService.queryProductOrderState(productMessage.getOutTradeNo());
            if (jsonData.getCode() == 0) {
                String state = jsonData.getData().toString();
                if (ProductOrderStateEnum.NEW.name().equalsIgnoreCase(state)) {
                    //่ฎขๅ็ถๆๆฏNEWๆฐๅปบ๏ผๅ่ฟๅ็ปๆถๆฏ้ๅ้ๆฐๆ้
                    log.warn("่ฎขๅ็ถๆๆฏNEW๏ผ่ฟๅ็ปๆถๆฏ้ๅ๏ผ้ๆฐๆ้:{}", productMessage);
                    return false;
                }
                if (ProductOrderStateEnum.PAY.name().equalsIgnoreCase(state)) {
                    //ๅฆๆๅทฒ็ปๆฏไป๏ผไฟฎๆนtask็ถๆไธบfinish
                    productTaskDO.setLockState(StockTaskStateEnum.FINISH.name());
                    productTaskMapper.update(productTaskDO, new QueryWrapper<ProductTaskDO>().eq("id", productMessage.getTaskId()));
                    log.info("่ฎขๅๅทฒ็ปๆฏไป๏ผไฟฎๆนๅบๅญ้ๅฎๅทฅไฝๅFINISH็ถๆ:{}", productMessage);
                    return true;
                }
            }
            //่ฎขๅไธๅญๅจ๏ผๆ่่ฎขๅ่ขซๅๆถ๏ผ็กฎ่ฎคๆถๆฏ,ไฟฎๆนtask็ถๆไธบCANCEL
            log.warn("่ฎขๅไธๅญๅจ,ๆ่ฎขๅ่ขซๅๆถ๏ผ็กฎ่ฎคๆถๆฏ๏ผไฟฎๆนtask็ถๆไธบCANCEL,ๆขๅคๅๅๅบๅญ,message:{}", productMessage);
            productTaskDO.setLockState(StockTaskStateEnum.CANCEL.name());
            productTaskMapper.update(productTaskDO, new QueryWrapper<ProductTaskDO>().eq("id", productMessage.getTaskId()));
            //ๆขๅคๅๅๅบๅญ๏ผ้้ๅฎๅบๅญ็ๅผๅๅปๅฝๅ่ดญไนฐ็ๅผ
            productMapper.unlockProductStock(productTaskDO.getProductId(), productTaskDO.getBuyNum());
        } else {
            log.warn("ๅทฅไฝๅ็ถๆไธๆฏLOCK,state={},ๆถๆฏไฝ={}", productTaskDO.getLockState(), productMessage);
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
