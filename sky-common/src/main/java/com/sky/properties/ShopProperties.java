package com.sky.properties;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "sky.shop")
@Component
@Data
public class ShopProperties {
    private String address;
    private String city;
    private Integer maxDeliveryDistanceMeters;
}
