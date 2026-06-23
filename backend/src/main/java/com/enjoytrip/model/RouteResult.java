package com.enjoytrip.model;

import java.util.ArrayList;
import java.util.List;

public class RouteResult {
    private String planId;
    private List<RouteStop> orderedStops = new ArrayList<>();
    private List<RouteLeg> legs = new ArrayList<>();
    private int totalMinutes;
    private int totalDurationSec;
    private int totalDistanceMeters;

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public List<RouteStop> getOrderedStops() {
        return orderedStops;
    }

    public void setOrderedStops(List<RouteStop> orderedStops) {
        this.orderedStops = orderedStops;
    }

    public List<RouteLeg> getLegs() {
        return legs;
    }

    public void setLegs(List<RouteLeg> legs) {
        this.legs = legs;
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    public void setTotalMinutes(int totalMinutes) {
        this.totalMinutes = totalMinutes;
    }

    public int getTotalDurationSec() {
        return totalDurationSec;
    }

    public void setTotalDurationSec(int totalDurationSec) {
        this.totalDurationSec = totalDurationSec;
    }

    public int getTotalDistanceMeters() {
        return totalDistanceMeters;
    }

    public void setTotalDistanceMeters(int totalDistanceMeters) {
        this.totalDistanceMeters = totalDistanceMeters;
    }
}
