package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 金额以元为单位，使用两位小数字符串供页面直接展示。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyConfirmVO implements Serializable {
    private Long orderId;
    private String orderNumber;
    private String orderAmount;
    private Integer status;
    private Integer payStatus;
    private boolean deducted;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkOutTime;
}
