package com.enjoytrip.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AppProperties {

    @Value("${kakao.mobility.rest.key}")
    private String kakaoMobilityRestKey;

    @Value("${app.storage.dir:./storage}")
    private String storageDir;

    @Value("${app.route.max-waypoints:8}")
    private int routeMaxWaypoints;

    public String getKakaoMobilityRestKey() { return kakaoMobilityRestKey; }
    public String getStorageDir() { return storageDir; }
    public int getRouteMaxWaypoints() { return routeMaxWaypoints; }
}
