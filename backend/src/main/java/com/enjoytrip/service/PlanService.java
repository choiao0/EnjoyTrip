package com.enjoytrip.service;

import com.enjoytrip.model.Lodging;
import com.enjoytrip.model.Plan;
import com.enjoytrip.model.PlanItem;
import com.enjoytrip.repository.PlanRepository;
import com.enjoytrip.util.DateTimeUtil;
import com.enjoytrip.util.IdGenerator;

import java.util.List;

public class PlanService {
    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public List<Plan> findByUserId(String userId) {
        return planRepository.findByUserId(userId);
    }

    public Plan findOwnedPlan(String planId, String userId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("여행 계획을 찾을 수 없습니다."));
        if (!plan.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 여행 계획만 조회할 수 있습니다.");
        }
        return plan;
    }

    public Plan save(String userId, String title, String memo, Lodging lodging, List<PlanItem> items) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("일정 제목을 입력하세요.");
        }
        Plan plan = new Plan();
        plan.setId(IdGenerator.nextId());
        plan.setUserId(userId);
        plan.setTitle(title.trim());
        plan.setMemo(memo == null ? "" : memo.trim());
        plan.setLodging(lodging);
        plan.setItems(items);
        plan.setCreatedAt(DateTimeUtil.now());
        plan.setUpdatedAt(DateTimeUtil.now());
        planRepository.save(plan);
        return plan;
    }

    public Plan addItem(String planId, String userId, String contentId, String title, double lat, double lng) {
        Plan plan = findOwnedPlan(planId, userId);
        boolean exists = plan.getItems().stream().anyMatch(i -> i.getContentId().equals(contentId));
        if (exists) throw new IllegalArgumentException("이미 추가된 장소입니다.");
        PlanItem item = new PlanItem();
        item.setContentId(contentId);
        item.setTitle(title);
        item.setLat(lat);
        item.setLng(lng);
        plan.getItems().add(item);
        plan.setUpdatedAt(DateTimeUtil.now());
        planRepository.save(plan);
        return plan;
    }

    public Plan updateLodging(String planId, String userId, Lodging lodging) {
        Plan plan = findOwnedPlan(planId, userId);
        plan.setLodging(lodging);
        plan.setUpdatedAt(DateTimeUtil.now());
        planRepository.save(plan);
        return plan;
    }

    public Plan reorderItems(String planId, String userId, List<PlanItem> items) {
        Plan plan = findOwnedPlan(planId, userId);
        plan.setItems(items);
        plan.setUpdatedAt(DateTimeUtil.now());
        planRepository.save(plan);
        return plan;
    }

    public void delete(String planId, String userId) {
        findOwnedPlan(planId, userId);
        planRepository.delete(planId);
    }
}
