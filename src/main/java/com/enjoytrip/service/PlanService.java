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
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("관광지 조회에서 장소를 추가하세요.");
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

    public void delete(String planId, String userId) {
        findOwnedPlan(planId, userId);
        planRepository.delete(planId);
    }
}
