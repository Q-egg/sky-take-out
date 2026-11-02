package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.sky.context.BaseContext;
import com.sky.dto.FamilyConfirmDTO;
import com.sky.entity.Orders;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.FamilyConfirmMapper;
import com.sky.service.FamilyConfirmService;
import com.sky.vo.FamilyConfirmVO;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class FamilyConfirmServiceImpl implements FamilyConfirmService {
    // 保留现有 Orders 常量及真实付款流程；新取值仅用于家庭确认。
    private static final int FAMILY_METHOD = 3;
    private static final int NO_PAYMENT_REQUIRED = 3;

    @Autowired
    private FamilyConfirmMapper familyConfirmMapper;

    @Value("${sky.family.enabled:true}")
    private boolean enabled;

    @Value("${sky.family.password:123456}")
    private String password;
    @Autowired
    private WebSocketServer webSocketServer;


    public FamilyConfirmVO confirm(FamilyConfirmDTO request) {
        if (!enabled) {
            throw new OrderBusinessException("家庭模拟支付未开启");
        }
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new OrderBusinessException("请先登录");
        }
        if (request == null || !StringUtils.hasText(request.getOrderNumber())) {
            throw new OrderBusinessException("订单号不能为空");
        }
        if (!StringUtils.hasText(password) || request.getConfirmCode() == null ||
                !request.getConfirmCode().matches("[0-9]{6}") ||
                !password.equals(request.getConfirmCode())) {
            throw new OrderBusinessException("测试密码错误，请重新输入");
        }

        Orders order = findOrder(request.getOrderNumber(), userId);
        if (isConfirmedFamilyOrder(order)) {
            return result(order);
        }
        if (!Orders.PENDING_PAYMENT.equals(order.getStatus()) ||
                !Orders.UN_PAID.equals(order.getPayStatus()) || order.getCheckoutTime() != null) {
            throw new OrderBusinessException("订单当前状态不允许确认");
        }
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        if (order.getOrderTime() == null || !order.getOrderTime().isAfter(cutoff)) {
            throw new OrderBusinessException("订单已超时，请重新下单");
        }
        if (order.getAmount() == null || order.getAmount().signum() < 0) {
            throw new OrderBusinessException("订单金额异常");
        }

        // 单条条件更新是原子的，重复请求及并发取消不能覆盖已推进的状态。
        LocalDateTime checkoutTime = LocalDateTime.now();
        int changed = familyConfirmMapper.confirmPending(order.getId(), userId,
                checkoutTime.minusMinutes(15), checkoutTime);
        if (changed == 0) {
            Orders current = findOrder(request.getOrderNumber(), userId);
            if (isConfirmedFamilyOrder(current)) {
                return result(current);
            }
            throw new OrderBusinessException("订单状态已变化或已超时，请刷新后重试");
        }
        order.setStatus(Orders.TO_BE_CONFIRMED);
        order.setPayMethod(FAMILY_METHOD);
        order.setPayStatus(NO_PAYMENT_REQUIRED);
        order.setCheckoutTime(checkoutTime);

        //通过WebSocket向客户端浏览器推送消息 type orderId content
        Map map = new HashMap();
        map.put("type",1);
        map.put("orderId",order.getId());
        map.put("content","订单号:" + order.getNumber());

        String json = JSON.toJSONString(map);

        webSocketServer.sendToAllClient(json);


        return result(order);
    }

    private Orders findOrder(String orderNumber, Long userId) {
        Orders order = familyConfirmMapper.findOwnedOrder(orderNumber, userId);
        if (order == null) {
            throw new OrderBusinessException("订单不存在或不属于当前用户");
        }
        return order;
    }

    private boolean isConfirmedFamilyOrder(Orders order) {
        return Integer.valueOf(FAMILY_METHOD).equals(order.getPayMethod()) &&
                Integer.valueOf(NO_PAYMENT_REQUIRED).equals(order.getPayStatus()) &&
                order.getStatus() != null && order.getStatus() >= Orders.TO_BE_CONFIRMED &&
                order.getStatus() <= Orders.COMPLETED;
    }

    private FamilyConfirmVO result(Orders order) {
        return FamilyConfirmVO.builder()
                .orderId(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount().setScale(2).toPlainString())
                .status(order.getStatus())
                .payStatus(order.getPayStatus())
                .deducted(false)
                .checkOutTime(order.getCheckoutTime())
                .build();
    }
}
