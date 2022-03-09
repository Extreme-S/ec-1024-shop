package org.example.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("product")
public class ProductDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 封面图
     */
    private String coverImg;

    /**
     * 详情
     */
    private String detail;

    /**
     * 老价格
     */
    private BigDecimal oldPrice;

    /**
     * 新价格
     */
    private BigDecimal price;

    /**
     * 库存
     */
    private Integer stock;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 锁定库存
     */
    private Integer lockStock;


}
