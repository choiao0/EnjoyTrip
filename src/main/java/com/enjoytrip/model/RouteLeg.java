package com.enjoytrip.model;

import java.util.ArrayList;
import java.util.List;

public class RouteLeg {
    private String fromTitle;
    private String toTitle;
    private int minutes;
    private int durationSec;
    private int distanceMeters;
    private List<MapPoint> points = new ArrayList<>();

    public String getFromTitle() {
        return fromTitle;
    }

    public void setFromTitle(String fromTitle) {
        this.fromTitle = fromTitle;
    }

    public String getToTitle() {
        return toTitle;
    }

    public void setToTitle(String toTitle) {
        this.toTitle = toTitle;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public int getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(int durationSec) {
        this.durationSec = durationSec;
    }

    public int getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(int distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public List<MapPoint> getPoints() {
        return points;
    }

    public void setPoints(List<MapPoint> points) {
        this.points = points;
    }
}
