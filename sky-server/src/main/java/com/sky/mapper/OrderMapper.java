package com.sky.mapper;


import com.github.pagehelper.Page;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);


    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);


    /**
     * 查询历史订单
     */

    Page<OrderVO> getOrderHistory(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 查询订单详情
     * @param
     * @return
     */
    @Select("select * from order_detail  where order_id = #{orderId}")
    List<OrderDetail> getOrderDetail(Long orderId);

    /**
     * 根据id查询订单信息
     */
    @Select("select * from orders where id = #{id} and user_id = #{userId}")
    Orders getOrderById(Long id, Long userId);

    /**
     * 取消订单
     */
    @Update("update orders set status = 6 , cancel_reason = '用户主动取消' , cancel_time = LOCALTIME where id = #{id} and user_id = #{userId} and status in (1,2)")
    void cancelOrder(Long id,Long userId);

    /**
     * 订单搜索
     */
    Page<OrderVO> conditionSearchOrders(OrdersPageQueryDTO ordersPageQueryDTO);



    /**
     * 各个状态的订单数量统计
     */
    @Select("select " +
            "count(case when status = 2 then 1 end) as toBeConfirmed, " +
            "count(case when status = 3 then 1 end) as confirmed, " +
            "count(case when status = 4 then 1 end) as deliveryInProgress " +
            "from orders where status in (2, 3, 4)")
    OrderStatisticsVO getStatistics();


    /**
     * 管理端查询订单信息
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getAdminOrderById(Long id);


    /**
     * 接单
     */
    @Update("update orders set status = 3 where id = #{id} and status = 2")
    int confirmById(Long id);

    /**
     * 拒单
     */
    @Update("update orders set status = 6 ,rejection_reason = #{rejectionReason},cancel_time = LOCALTIME where id = #{id} and status = 2 ")
    int rejectById(OrdersRejectionDTO ordersRejectionDTO);


    /**
     * 商家取消订单
     */
    @Update("update orders set status = 6 ,cancel_reason = #{cancelReason} ,cancel_time = LOCALTIME where id = #{id} and status = 3")
    int cancelById(OrdersCancelDTO ordersCancelDTO);

    /**
     * 派送订单
     */
    @Update("update orders set status = 4 where id = #{id} and status = 3")
    int delivery(Long id);


    /**
     * 完成订单
     */
    @Update("update orders set status = 5,delivery_time = LOCALTIME where id = #{id} and status = 4")
    int complete(Long id);
}
