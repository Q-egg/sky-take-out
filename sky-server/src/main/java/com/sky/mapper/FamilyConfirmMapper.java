package com.sky.mapper;

import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/** 独立于原微信支付 Mapper，3 分别表示家庭确认、无需支付。 */
@Mapper
public interface FamilyConfirmMapper {
    @Select("select * from orders where number = #{orderNumber} and user_id = #{userId}")
    Orders findOwnedOrder(@Param("orderNumber") String orderNumber, @Param("userId") Long userId);

    @Update("update orders set status = 2, pay_method = 3, pay_status = 3, checkout_time = #{checkoutTime} " +
            "where id = #{id} and user_id = #{userId} and status = 1 and pay_status = 0 " +
            "and checkout_time is null and order_time > #{cutoff}")
    int confirmPending(@Param("id") Long id, @Param("userId") Long userId,
                       @Param("cutoff") LocalDateTime cutoff, @Param("checkoutTime") LocalDateTime checkoutTime);
}
