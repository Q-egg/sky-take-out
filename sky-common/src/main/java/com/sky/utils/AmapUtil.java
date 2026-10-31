package com.sky.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.exception.BaseException;
import com.sky.properties.AmapProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class AmapUtil {

    private static final String GEOCODE_URL =
            "https://restapi.amap.com/v3/geocode/geo";

    private static final String ELECTROBIKE_URL =
            "https://restapi.amap.com/v5/direction/electrobike";

    @Autowired
    private AmapProperties amapProperties;

    /**
     * 将文字地址转换为高德坐标。
     *
     * @return 经度,纬度
     */
    public String geocode(String address, String city) {
        if (!StringUtils.hasText(address)) {
            throw new BaseException("地址不能为空");
        }

        if (!StringUtils.hasText(amapProperties.getKey())) {
            throw new BaseException("高德 Key 未配置");
        }

        // 1. 准备接口参数
        Map<String, String> params = new HashMap<>();
        params.put("key", amapProperties.getKey());
        params.put("address", address.trim());
        params.put("output", "JSON");

        if (StringUtils.hasText(city)) {
            params.put("city", city.trim());
        }

        // 2. 发送请求
        String response;
        try {
            response = HttpClientUtil.doGet(GEOCODE_URL, params);
        } catch (RuntimeException e) {
            log.warn("高德地理编码请求未完成");
            throw new BaseException("地图服务暂时不可用，请稍后重试");
        }

        // 3. 解析响应
        JSONObject result;
        try {
            result = JSONObject.parseObject(response);
        } catch (RuntimeException e) {
            throw new BaseException("地图服务返回的数据格式异常");
        }

        if (result == null) {
            throw new BaseException("地图服务返回的数据为空");
        }

        // HTTP 成功不代表高德业务处理成功
        if (!"1".equals(result.getString("status"))) {
            log.warn("高德地理编码失败，错误码：{}",
                    result.getString("infocode"));
            throw new BaseException("地址解析服务异常，请稍后重试");
        }

        // 4. 检查匹配结果
        JSONArray geocodes = result.getJSONArray("geocodes");

        if (geocodes == null || geocodes.isEmpty()) {
            throw new BaseException("未找到该地址，请完善地址信息");
        }


        // 5. 取出坐标
        JSONObject geocode = geocodes.getJSONObject(0);
        String location = geocode.getString("location");

        if (!StringUtils.hasText(location)) {
            throw new BaseException("该地址未返回有效坐标");
        }

        return location;
    }

    /**
     * 查询电动车路线距离。
     *
     * @param origin      起点坐标，经度,纬度
     * @param destination 终点坐标，经度,纬度
     * @return 路线距离，单位米
     */

    public long getElectrobikeDistance(String origin, String destination) {
        if (!StringUtils.hasText(origin)
                || !StringUtils.hasText(destination)) {
            throw new BaseException("起点和终点坐标不能为空");
        }

        if (!StringUtils.hasText(amapProperties.getKey())) {
            throw new BaseException("高德 Key 未配置");
        }

        // 1. 准备请求参数
        Map<String, String> params = new HashMap<>();
        params.put("key", amapProperties.getKey());
        params.put("origin", origin.trim());
        params.put("destination", destination.trim());
        params.put("output", "JSON");
        params.put("alternative_route", "1");

        // 2. 调用高德接口
        String response;
        try {
            response = HttpClientUtil.doGet(ELECTROBIKE_URL, params);
        } catch (RuntimeException e) {
            log.warn("高德电动车路线请求未完成");
            throw new BaseException("地图服务暂时不可用，请稍后重试");
        }

        // 3. 解析响应
        JSONObject result;
        try {
            result = JSONObject.parseObject(response);
        } catch (RuntimeException e) {
            throw new BaseException("地图服务返回的数据格式异常");
        }

        if (result == null) {
            throw new BaseException("地图服务返回的数据为空");
        }

        if (!"1".equals(result.getString("status"))) {
            log.warn("高德电动车路线规划失败，错误码：{}",
                    result.getString("infocode"));
            throw new BaseException("配送路线查询失败，请稍后重试");
        }

        // 4. 取得路线方案
        JSONObject route = result.getJSONObject("route");
        if (route == null) {
            throw new BaseException("未查询到可用的配送路线");
        }

        JSONArray paths = route.getJSONArray("paths");
        if (paths == null || paths.isEmpty()) {
            throw new BaseException("未查询到可用的配送路线");
        }

        JSONObject path = paths.getJSONObject(0);
        if (path == null) {
            throw new BaseException("配送路线数据为空");
        }

        // 5. 将距离转换为数值
        String distanceText = path.getString("distance");
        if (!StringUtils.hasText(distanceText)) {
            throw new BaseException("配送路线未返回距离");
        }

        long distance;
        try {
            distance = Long.parseLong(distanceText);
        } catch (NumberFormatException e) {
            throw new BaseException("配送距离格式异常");
        }

        if (distance < 0) {
            throw new BaseException("配送距离不能为负数");
        }

        return distance;
    }
}