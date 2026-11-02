package com.sky.dto;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/** 家庭模拟付款请求；确认码不是微信支付密码。 */
@Data
public class FamilyConfirmDTO implements Serializable {
    private String orderNumber;

    @ToString.Exclude
    private String confirmCode;
}
