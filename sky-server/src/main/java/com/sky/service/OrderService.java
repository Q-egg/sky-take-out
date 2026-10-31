package com.sky.service;

import com.sky.dto.*;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {


    /**
     * 用户下单
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);


    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);


    /**
     * 查询历史订单
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult getOrderHistory(OrdersPageQueryDTO ordersPageQueryDTO);


    /**
     * 查询订单详情
     */
    OrderVO getOrderDetail(Long id);


    /**
     * 取消订单
     */
    void cancelOrder(Long id);


    /**
     * 再来一单
     * @param id
     */
    void repetitionOrder(Long id);


    /**
     * 订单搜索
     */
    PageResult  conditionSearchOrders(OrdersPageQueryDTO ordersPageQueryDTO);


    /**
     * 各状态订单状态统计
     * @return
     */
    OrderStatisticsVO getStatistics();


    /**
     * 管理端查询订单详情
     */
    OrderVO getAdminOrderDetail(Long id);

    /**
     * 接单
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);


    /**
     * 拒单
     * @param
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO);


    /**
     * 商家取消订单
     * @param ordersCancelDTO
     */
    void cancelById(OrdersCancelDTO ordersCancelDTO);

    /**
     * 派送订单
     */
    void delivery(Long id);

    void complete(Long id);
}
