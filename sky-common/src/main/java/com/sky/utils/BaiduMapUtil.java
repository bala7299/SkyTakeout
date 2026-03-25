package com.sky.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.exception.OrderBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class BaiduMapUtil {
    //获得百度地图的ak
    @Value("${sky.baidu.ak}")
    private String ak;
    //获得百度地图的url
    private static final String GEOCODING_URL = "https://api.map.baidu.com/geocoding/v3";
    private static final String DIRECTION_URL = "https://api.map.baidu.com/directionlite/v1/riding";

    //获得经纬度方法
    public String getCoordinate(String address)  {
        Map<String, String> map = new HashMap<>();
        map.put("address", address);
        map.put("output", "json");
        map.put("ak", ak);
        String response = HttpClientUtil.doGet(GEOCODING_URL, map);
        JSONObject jsonObject = JSONObject.parseObject(response);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("获取经纬度失败");
        }
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        return lat + "," + lng;
    }

    //获得距离方法
    public Integer getDistance(String origin, String destination) {
        Map<String, String> map = new HashMap<>();
        map.put("origin", origin);           // 起点坐标
        map.put("destination", destination); // 终点坐标
        map.put("steps_info", "0");
        map.put("ak", ak);
        String response = HttpClientUtil.doGet(DIRECTION_URL, map);
        JSONObject jsonObject = JSONObject.parseObject(response);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("获取距离失败");
        }
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray distanceJson = result.getJSONArray("routes");
        return distanceJson.getJSONObject(0).getInteger("distance");
    }
}
