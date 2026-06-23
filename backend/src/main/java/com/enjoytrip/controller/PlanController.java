package com.enjoytrip.controller;

import com.enjoytrip.model.DraftPlan;
import com.enjoytrip.model.Lodging;
import com.enjoytrip.model.Plan;
import com.enjoytrip.model.PlanItem;
import com.enjoytrip.model.RouteResult;
import com.enjoytrip.service.PlanService;
import com.enjoytrip.service.RouteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plans")
public class PlanController extends BaseController {

    @Autowired
    private PlanService planService;

    @Autowired
    private RouteService routeService;

    // ── 저장된 계획 ──────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<Plan>> list(HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(planService.findByUserId(currentUser(session).getId()));
    }

    @PostMapping
    public ResponseEntity<Plan> save(@RequestBody Map<String, Object> body, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String title = (String) body.get("title");
        String memo = (String) body.get("memo");
        Lodging lodging = buildLodging(body);
        List<PlanItem> items;
        Object rawItems = body.get("items");
        if (rawItems instanceof List<?> rawList) {
            items = parseBodyItems(rawList);
        } else {
            DraftPlan draft = draftPlan(session);
            items = new ArrayList<>(draft.getItems());
            draft.getItems().clear();
        }
        Plan plan = planService.save(currentUser(session).getId(), title, memo, lodging, items);
        return ResponseEntity.status(HttpStatus.CREATED).body(plan);
    }

    @PatchMapping("/{id}/lodging")
    public ResponseEntity<Plan> updateLodging(@PathVariable String id, @RequestBody Map<String, Object> body,
                                              HttpSession session) {
        if (!isLoggedIn(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Object lat = body.get("lat");
        Object lng = body.get("lng");
        if (lat == null || lng == null) throw new IllegalArgumentException("숙소 좌표를 입력하세요.");
        Lodging lodging = new Lodging();
        lodging.setLat(((Number) lat).doubleValue());
        lodging.setLng(((Number) lng).doubleValue());
        lodging.setAddress((String) body.get("address"));
        lodging.setPlaceName((String) body.get("placeName"));
        Plan plan = planService.updateLodging(id, currentUser(session).getId(), lodging);
        return ResponseEntity.ok(plan);
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<Plan> addItem(@PathVariable String id, @RequestBody Map<String, Object> body,
                                        HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String contentId = (String) body.get("contentId");
        String title = (String) body.get("title");
        double lat = ((Number) body.get("lat")).doubleValue();
        double lng = ((Number) body.get("lng")).doubleValue();
        Plan plan = planService.addItem(id, currentUser(session).getId(), contentId, title, lat, lng);
        return ResponseEntity.ok(plan);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        planService.delete(id, currentUser(session).getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/recommend")
    public ResponseEntity<RouteResult> recommend(@PathVariable String id, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Plan plan = planService.findOwnedPlan(id, currentUser(session).getId());
        RouteResult routeResult = routeService.recommend(plan);
        return ResponseEntity.ok(routeResult);
    }

    // ── 임시 계획 (Draft) ────────────────────────────────────────

    @GetMapping("/draft")
    public DraftPlan getDraft(HttpSession session) {
        return draftPlan(session);
    }

    @PostMapping("/draft/items")
    public ResponseEntity<Map<String, Object>> addDraftItem(@RequestBody Map<String, Object> body,
                                                             HttpSession session) {
        String contentId = (String) body.get("contentId");
        String title = (String) body.get("title");
        double lat = ((Number) body.get("lat")).doubleValue();
        double lng = ((Number) body.get("lng")).doubleValue();

        DraftPlan draft = draftPlan(session);
        boolean exists = draft.getItems().stream().anyMatch(i -> i.getContentId().equals(contentId));
        if (exists) {
            return ResponseEntity.ok(Map.of("ok", false, "message", "이미 추가된 장소입니다."));
        }
        PlanItem item = new PlanItem();
        item.setContentId(contentId);
        item.setTitle(title);
        item.setLat(lat);
        item.setLng(lng);
        draft.getItems().add(item);
        return ResponseEntity.ok(Map.of("ok", true, "message", "여행 계획에 장소를 추가했습니다."));
    }

    @DeleteMapping("/draft/items/{index}")
    public ResponseEntity<Void> removeDraftItem(@PathVariable int index, HttpSession session) {
        DraftPlan draft = draftPlan(session);
        if (index >= 0 && index < draft.getItems().size()) {
            draft.getItems().remove(index);
        }
        return ResponseEntity.ok().build();
    }

    @PutMapping("/draft/items/{index}/move")
    public ResponseEntity<Void> moveDraftItem(@PathVariable int index,
                                               @RequestBody Map<String, String> body,
                                               HttpSession session) {
        String direction = body.get("direction");
        List<PlanItem> items = draftPlan(session).getItems();
        int next = "up".equals(direction) ? index - 1 : index + 1;
        if (index >= 0 && next >= 0 && index < items.size() && next < items.size()) {
            PlanItem tmp = items.get(index);
            items.set(index, items.get(next));
            items.set(next, tmp);
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/draft")
    public ResponseEntity<Void> clearDraft(HttpSession session) {
        draftPlan(session).getItems().clear();
        return ResponseEntity.ok().build();
    }

    @SuppressWarnings("unchecked")
    private List<PlanItem> parseBodyItems(List<?> rawList) {
        List<PlanItem> items = new ArrayList<>();
        for (Object o : rawList) {
            if (!(o instanceof Map)) continue;
            Map<String, Object> m = (Map<String, Object>) o;
            PlanItem item = new PlanItem();
            item.setContentId((String) m.get("contentId"));
            item.setTitle((String) m.get("title"));
            item.setLat(((Number) m.get("lat")).doubleValue());
            item.setLng(((Number) m.get("lng")).doubleValue());
            items.add(item);
        }
        return items;
    }

    @SuppressWarnings("unchecked")
    private Lodging buildLodging(Map<String, Object> body) {
        Object lodgingObj = body.get("lodging");
        if (!(lodgingObj instanceof Map)) return null;
        Map<String, Object> m = (Map<String, Object>) lodgingObj;
        Object lat = m.get("lat");
        Object lng = m.get("lng");
        if (lat == null || lng == null) return null;
        Lodging lodging = new Lodging();
        lodging.setAddress((String) m.get("address"));
        lodging.setPlaceName((String) m.get("placeName"));
        lodging.setLat(((Number) lat).doubleValue());
        lodging.setLng(((Number) lng).doubleValue());
        return lodging;
    }
}
