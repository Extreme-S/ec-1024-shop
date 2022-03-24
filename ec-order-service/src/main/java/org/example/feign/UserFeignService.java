package org.example.feign;

import org.example.util.JsonData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ec-user-service")
public interface UserFeignService {

    @GetMapping("/api/address/v1//find/{address_id}")
    JsonData detail(@PathVariable("address_id") long addressId);

}
