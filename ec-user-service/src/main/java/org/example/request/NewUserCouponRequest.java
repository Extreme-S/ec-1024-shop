package org.example.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NewUserCouponRequest {

    @JsonProperty("user_id")
    private long userId;

    @JsonProperty("name")
    private String name;

}
