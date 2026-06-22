package com.enjoytrip.model;

import java.util.ArrayList;
import java.util.List;

public class DraftPlan {
    private List<PlanItem> items = new ArrayList<>();

    public List<PlanItem> getItems() {
        return items;
    }

    public void setItems(List<PlanItem> items) {
        this.items = items;
    }
}
