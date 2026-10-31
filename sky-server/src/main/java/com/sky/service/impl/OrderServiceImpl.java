package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.properties.ShopProperties;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.service.ShoppingCartService;
import com.sky.utils.AmapUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AmapUtil amapUtil;

    @Autowired
    private ShopProperties shopProperties;




    /**
     * 用户下单
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        Long userId = BaseContext.getCurrentId();

        // 1. 校验地址是否存在
        AddressBook addressBook =
                addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());

        if (addressBook == null) {
            throw new AddressBookBusinessException(
                    MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 2. 校验地址是否属于当前用户
        if (userId == null || !userId.equals(addressBook.getUserId())) {
            throw new AddressBookBusinessException("无权使用该收货地址");
        }

        // 3. 校验购物车
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);

        List<ShoppingCart> shoppingCartListlist =
                shoppingCartMapper.list(shoppingCart);

        if (shoppingCartListlist == null || shoppingCartListlist.isEmpty()) {
            throw new ShoppingCartBusinessException(
                    MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 4. 校验配送范围，通过后才执行后续订单保存
        checkDeliveryRange(addressBook);


        //2. 向订单表插入1条数据

        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);

        // 保存下单时的完整地址
        orders.setAddress(
                addressPart(addressBook.getProvinceName()) +
                        addressPart(addressBook.getCityName()) +
                        addressPart(addressBook.getDistrictName()) +
                        addressPart(addressBook.getDetail())
        );

        orderMapper.insert(orders);


        List<OrderDetail> orderDetailList = new ArrayList<>();
        //3. 向订单明细表插入n条数据
        for(ShoppingCart cart:shoppingCartListlist){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());  //设置当前订单明细关联的订单id
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        // 4. 清空购物车
        shoppingCartMapper.deleteByUserId(userId);


        //5. 封装VO返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder().id(orders.getId()).orderTime(orders.getOrderTime()).orderNumber(orders.getNumber()).orderAmount(orders.getAmount()).build();
        return orderSubmitVO;

    }


    /**
     * 校验收货地址是否在配送范围内。
     */
    private void checkDeliveryRange(AddressBook addressBook) {
        // 1. 检查门店配置
        Integer maxDistance =
                shopProperties.getMaxDeliveryDistanceMeters();

        if (maxDistance == null || maxDistance <= 0) {
            throw new OrderBusinessException("门店配送距离配置异常");
        }

        if (!StringUtils.hasText(shopProperties.getAddress())) {
            throw new OrderBusinessException("门店地址未配置");
        }


        // 先校验城市，跨城不调用地图接口
        String shopCity = shopProperties.getCity();
        String deliveryCity = addressBook.getCityName();

        if (!StringUtils.hasText(shopCity)) {
            throw new OrderBusinessException("门店城市未配置");
        }

        if (!StringUtils.hasText(deliveryCity)) {
            throw new AddressBookBusinessException("请完善收货地址的城市信息");
        }

        if (!normalizeCity(shopCity).equals(normalizeCity(deliveryCity))) {
            throw new OrderBusinessException(
                    MessageConstant.DELIVERY_CITY_OUT);
        }


        // 2. 检查并拼接收货地址
        if (!StringUtils.hasText(addressBook.getDetail())) {
            throw new AddressBookBusinessException("请完善收货详细地址");
        }

        String deliveryAddress =
                addressPart(addressBook.getProvinceName()) +
                        addressPart(addressBook.getCityName()) +
                        addressPart(addressBook.getDistrictName()) +
                        addressPart(addressBook.getDetail());

        // 3. 获取门店坐标
        String origin = amapUtil.geocode(
                shopProperties.getAddress(),
                shopProperties.getCity()
        );

        // 4. 获取收货地址坐标
        String destination = amapUtil.geocode(
                deliveryAddress,
                addressBook.getCityName()
        );

        // 5. 获取电动车路线距离，单位米
        long distance =
                amapUtil.getElectrobikeDistance(origin, destination);

        // 6. 超出配送范围时阻止下单
        if (distance > maxDistance) {
            throw new OrderBusinessException(MessageConstant.DELIVERY_OUT);
        }
    }


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 查询历史订单
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult getOrderHistory(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        //查询所有的订单
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        Page<OrderVO> orders = orderMapper.getOrderHistory(ordersPageQueryDTO);
        //为每个订单添加对应的详细数据
        for(OrderVO order:orders){
            List<OrderDetail> orderDetail = orderMapper.getOrderDetail(order.getId());
            order.setOrderDetailList(orderDetail);
        }
        PageResult pageResult = new PageResult();
        pageResult.setTotal(orders.getTotal());
        pageResult.setRecords(orders.getResult());

        return pageResult;
    }

    /**
     * 查询订单详情
     */
    public OrderVO getOrderDetail(Long id) {

        //查询订单信息
        Long userId = BaseContext.getCurrentId();
        Orders order = orderMapper.getOrderById(id,userId);

        if(order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //查询订单详情
        List<OrderDetail> orderDetail = orderMapper.getOrderDetail(id);


        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        orderVO.setOrderDetailList(orderDetail);
        return orderVO;
    }

    /**
     * 取消订单
     */
    public void cancelOrder(Long id) {
        Long userId = BaseContext.getCurrentId();
        Orders orderById = orderMapper.getOrderById(id, userId);
        if(orderById == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        orderMapper.cancelOrder(id,userId);

        //真实业务订单还要加上退款处理


    }

    /**
     * 再来一单
     * @param id
     */

    public void repetitionOrder(Long id) {
        Orders orderById = orderMapper.getOrderById(id, BaseContext.getCurrentId());
        if(orderById == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        List<OrderDetail> orderDetail = orderMapper.getOrderDetail(id);
        if(orderDetail == null || orderDetail.size() == 0){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Long userId = BaseContext.getCurrentId();
        LocalDateTime now = LocalDateTime.now();
        //插入购物车
        for (OrderDetail detail : orderDetail) {
            // 每次循环都创建一个新的购物车对象
            ShoppingCart cart = new ShoppingCart();

            // 从订单明细拷贝到购物车，忽略原来的主键 id
            BeanUtils.copyProperties(detail, cart, "id");

            cart.setUserId(userId);
            cart.setCreateTime(now);
            shoppingCartMapper.insert(cart);
        }



    }

    /**
     * 订单搜索
     */
    public PageResult conditionSearchOrders(OrdersPageQueryDTO ordersPageQueryDTO) {

        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        //查询订单
        Page<OrderVO>  page = orderMapper.conditionSearchOrders(ordersPageQueryDTO);

        //查询订单菜品信息
        for(OrderVO orderVO:page){
            List<OrderDetail> orderDetail = orderMapper.getOrderDetail(orderVO.getId());
            String orderDishes = orderDetail.stream()
                    .map(item -> item.getName() + " × " + item.getNumber())
                    .collect(Collectors.joining("，"));
            orderVO.setOrderDishes(orderDishes);
        }
        PageResult pageResult = new PageResult();
        pageResult.setTotal(page.getTotal());
        pageResult.setRecords(page.getResult());
        return pageResult;



    }

    /**
     * 各状态订单状态统计
     */
    public OrderStatisticsVO getStatistics() {
        return orderMapper.getStatistics();
    }

    /**
     * 管理端查询订单详情
     */
    public OrderVO getAdminOrderDetail(Long id) {
        //查询订单信息
        Orders order = orderMapper.getAdminOrderById(id);

        //查询订单详细信息
        List<OrderDetail> orderDetail = orderMapper.getOrderDetail(id);

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        orderVO.setOrderDetailList(orderDetail);
        return orderVO;
    }

    /**
     * 接单
     * @param
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {

        int i = orderMapper.confirmById(ordersConfirmDTO.getId());
        if(i == 0){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    /**
     * 拒单
     * @param
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        int i = orderMapper.rejectById(ordersRejectionDTO);
        if(i == 0){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

    }

    /**
     * 商家取消订单
     */
    public void cancelById(OrdersCancelDTO ordersCancelDTO) {
        int i = orderMapper.cancelById(ordersCancelDTO);
        if(i == 0){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

    }

    /**
     * 派送订单
     * @param id
     */
    public void delivery(Long id) {
        int delivery = orderMapper.delivery(id);
        if(delivery == 0){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

    }

    /**
     * 完成订单
     * @param id
     */
    public void complete(Long id) {
        int complete = orderMapper.complete(id);
        if(complete == 0){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

    }

    private String addressPart(String value) {
        return value == null ? "" : value;
    }

    /**
     * 统一城市名称：例如“西安市”和“西安”统一为“西安”。
     */
    private String normalizeCity(String city) {
        String name = city.trim();

        if (name.endsWith("市")) {
            return name.substring(0, name.length() - 1);
        }

        return name;
    }
}
