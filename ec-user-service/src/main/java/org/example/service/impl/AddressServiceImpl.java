package org.example.service.impl;

import org.example.model.AddressDO;
import org.example.mapper.AddressMapper;
import org.example.service.AddressService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 电商-公司收发货地址表 服务实现类
 * </p>
 *
 * @author 不爱吃鱼的猫丶
 * @since 2021-10-03
 */
@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, AddressDO> implements AddressService {

}
