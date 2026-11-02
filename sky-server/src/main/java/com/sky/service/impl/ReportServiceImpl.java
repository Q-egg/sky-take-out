package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.models.auth.In;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额统计
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放从begin到end之间的日期
        List<LocalDate> dataList = new ArrayList<>();
        dataList.add(begin);
        while(!begin.equals(end)){
            //日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dataList.add(begin);
        }

        //查询每一天的营业额
        List<Double> turnoverList = new ArrayList<>();
        for(LocalDate date : dataList){
            //查询data日期对应的营业额数据，营业额是指：状态为“已完成”的订单金额合计
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }


        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dataList,  ","))
                .turnoverList(StringUtils.join(turnoverList,  ","))
                .build();
    }

    /**
     * 用户统计
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放从begin到end之间的日期
        List<LocalDate> dataList = new ArrayList<>();
        dataList.add(begin);
        while(!begin.equals(end)){
            //日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dataList.add(begin);
        }

        //查询每一天的新增用户数量以及截至到该天的用户总量
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        for(LocalDate date : dataList){

            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("end", endTime);
            Integer totalUser = userMapper.countByMap(map);
            map.put("begin", beginTime);
            Integer newUser = userMapper.countByMap(map);
            newUser = newUser == null ? 0 : newUser;
            newUserList.add(newUser);
            totalUserList.add(totalUser);
        }


        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dataList,  ","))
                .newUserList(StringUtils.join(newUserList,  ","))
                .totalUserList(StringUtils.join(totalUserList,  ","))
                .build();
    }
}
