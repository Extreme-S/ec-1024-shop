package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.mapper.ProductMapper;
import org.example.model.ProductDO;
import org.example.service.ProductService;
import org.example.vo.ProductVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

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

    private ProductVO beanProcess(ProductDO productDO) {
        ProductVO productVO = new ProductVO();
        BeanUtils.copyProperties(productDO, productVO);
        productVO.setStock(productDO.getStock() - productDO.getLockStock());
        return productVO;
    }
}
