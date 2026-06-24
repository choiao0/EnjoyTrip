package com.enjoytrip.controller;

import com.enjoytrip.model.GroupMember;
import com.enjoytrip.model.GroupPlace;
import com.enjoytrip.model.GroupTrip;
import com.enjoytrip.model.User;
import com.enjoytrip.service.GroupTripService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class GroupTripController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(GroupTripController.class);

    @Autowired
    private GroupTripService groupTripService;

    @Autowired
    private SimpMessagingTemplate messaging;

    // ── 그룹 목록 / 생성 ───────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<GroupTrip>> list(HttpSession session) {
        log.info("GET /api/groups 요청 (로그인: {})", isLoggedIn(session));
        if (!isLoggedIn(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(groupTripService.findMyGroups(currentUser(session).getId()));
    }

    @PostMapping
    public ResponseEntity<GroupTrip> create(@RequestBody Map<String, String> body, HttpSession session) {
        log.info("POST /api/groups 요청 title={} (로그인: {})", body.get("title"), isLoggedIn(session));
        if (!isLoggedIn(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User user = currentUser(session);
        GroupTrip trip = groupTripService.createGroup(user.getId(), user.getName(), body.get("title"), body.get("description"));
        return ResponseEntity.status(HttpStatus.CREATED).body(trip);
    }

    @PostMapping("/join-by-code")
    public ResponseEntity<Map<String, Object>> joinByCode(@RequestBody Map<String, String> body, HttpSession session) {
        if (!isLoggedIn(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User user = currentUser(session);
        String code = body.get("code");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("초대 코드를 입력하세요.");
        Map<String, Object> result = groupTripService.joinByCode(code.trim().toUpperCase(), user.getId(), user.getName());
        GroupTrip trip = (GroupTrip) result.get("group");
        GroupMember member = (GroupMember) result.get("member");
        messaging.convertAndSend("/topic/group/" + trip.getId(),
                Map.of("type", "MEMBER_JOINED", "actorId", user.getId(), "data", member));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // ── 그룹 상세 / 삭제 ───────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long id, HttpSession session) {
        log.info("GET /api/groups/{} 요청 (로그인: {})", id, isLoggedIn(session));
        if (!isLoggedIn(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Map<String, Object> detail = groupTripService.findDetail(id, currentUser(session).getId());
        return ResponseEntity.ok(detail);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpSession session) {
        log.info("DELETE /api/groups/{} 요청 (로그인: {})", id, isLoggedIn(session));
        if (!isLoggedIn(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        groupTripService.deleteGroup(id, currentUser(session).getId());
        return ResponseEntity.noContent().build();
    }

    // ── 멤버 참가 / 탈퇴 / 추방 ────────────────────────────────────

    @PostMapping("/{id}/join")
    public ResponseEntity<GroupMember> join(@PathVariable Long id, HttpSession session) {
        if (!isLoggedIn(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User user = currentUser(session);
        log.info("그룹 참가 요청 groupId={} userId={}", id, user.getId());
        GroupMember member = groupTripService.joinGroup(id, user.getId(), user.getName());
        messaging.convertAndSend("/topic/group/" + id,
                Map.of("type", "MEMBER_JOINED", "actorId", user.getId(), "data", member));
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leave(@PathVariable Long id, HttpSession session) {
        if (!isLoggedIn(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User user = currentUser(session);
        log.info("그룹 탈퇴 요청 groupId={} userId={}", id, user.getId());
        groupTripService.leaveGroup(id, user.getId());
        messaging.convertAndSend("/topic/group/" + id,
                Map.of("type", "MEMBER_LEFT", "actorId", user.getId(), "data", Map.of("userId", user.getId())));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> kickMember(@PathVariable Long id,
                                            @PathVariable String userId,
                                            HttpSession session) {
        if (!isLoggedIn(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String requesterId = currentUser(session).getId();
        log.info("멤버 추방 요청 groupId={} targetUserId={} by={}", id, userId, requesterId);
        groupTripService.kickMember(id, userId, requesterId);
        messaging.convertAndSend("/topic/group/" + id,
                Map.of("type", "MEMBER_LEFT", "actorId", requesterId, "data", Map.of("userId", userId)));
        return ResponseEntity.noContent().build();
    }

    // ── 장소 추가 / 삭제 ────────────────────────────────────────────

    @PostMapping("/{id}/places")
    public ResponseEntity<GroupPlace> addPlace(@PathVariable Long id,
                                               @RequestBody Map<String, Object> body,
                                               HttpSession session) {
        if (!isLoggedIn(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User user = currentUser(session);
        GroupPlace place = new GroupPlace();
        place.setContentId((String) body.get("contentId"));
        place.setTitle((String) body.get("title"));
        place.setLat(((Number) body.get("lat")).doubleValue());
        place.setLng(((Number) body.get("lng")).doubleValue());
        log.info("장소 추가 요청 groupId={} title={} by={}", id, place.getTitle(), user.getId());
        GroupPlace saved = groupTripService.addPlace(id, user.getId(), user.getName(), place);
        messaging.convertAndSend("/topic/group/" + id,
                Map.of("type", "PLACE_ADDED", "actorId", user.getId(), "data", saved));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}/places/{placeId}")
    public ResponseEntity<Void> removePlace(@PathVariable Long id,
                                             @PathVariable Long placeId,
                                             HttpSession session) {
        if (!isLoggedIn(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User user = currentUser(session);
        log.info("장소 삭제 요청 groupId={} placeId={} by={}", id, placeId, user.getId());
        groupTripService.removePlace(id, placeId, user.getId());
        messaging.convertAndSend("/topic/group/" + id,
                Map.of("type", "PLACE_REMOVED", "actorId", user.getId(), "data", Map.of("placeId", placeId)));
        return ResponseEntity.noContent().build();
    }
}
