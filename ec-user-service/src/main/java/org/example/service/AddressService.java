package org.example.service;

import java.util.List;
import org.example.model.AddressDO;
import org.example.request.AddressAddRequest;
import org.example.vo.AddressVO;


public interface AddressService {

    /**
     * 查找指定地址详情
     */
    AddressVO detail(Long id);

    /**
     * 新增收货地址
     */
    void add(AddressAddRequest addressAddRequest);

    /**
     * 根据id删除地址
     */
    int del(int addressId);

    /**
     * 查找用户全部收货地址
     */
    List<AddressVO> listUserAllAddress();

}
