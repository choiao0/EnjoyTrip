package com.enjoytrip.service;

import com.enjoytrip.model.GroupMember;
import com.enjoytrip.model.GroupPlace;
import com.enjoytrip.model.GroupTrip;
import com.enjoytrip.repository.GroupTripRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GroupTripService {

    private final GroupTripRepository repo;

    public GroupTripService(GroupTripRepository repo) {
        this.repo = repo;
    }

    public List<GroupTrip> findMyGroups(String userId) {
        return repo.findMyGroups(userId);
    }

    public Map<String, Object> findDetail(Long groupId, String userId) {
        GroupTrip trip = repo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));
        List<GroupMember> members = repo.findMembers(groupId);
        List<GroupPlace> places = repo.findPlaces(groupId);
        boolean isMember = repo.isMember(groupId, userId);
        boolean isHost = trip.getHostUserId().equals(userId);
        return Map.of(
                "group", trip,
                "members", members,
                "places", places,
                "isMember", isMember,
                "isHost", isHost
        );
    }

    public GroupTrip createGroup(String userId, String userName, String title, String description) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("그룹 이름을 입력하세요.");
        }
        GroupTrip trip = new GroupTrip();
        trip.setTitle(title.trim());
        trip.setHostUserId(userId);
        trip.setHostUserName(userName);
        trip.setDescription(description == null ? "" : description.trim());
        repo.save(trip);
        repo.addMember(trip.getId(), userId, userName);
        return trip;
    }

    public void deleteGroup(Long groupId, String userId) {
        GroupTrip trip = repo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));
        if (!trip.getHostUserId().equals(userId)) {
            throw new IllegalArgumentException("그룹장만 삭제할 수 있습니다.");
        }
        repo.delete(groupId);
    }

    public GroupMember joinGroup(Long groupId, String userId, String userName) {
        repo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));
        if (repo.isMember(groupId, userId)) {
            throw new IllegalArgumentException("이미 그룹 멤버입니다.");
        }
        return repo.addMember(groupId, userId, userName);
    }

    public void leaveGroup(Long groupId, String userId) {
        GroupTrip trip = repo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));
        if (trip.getHostUserId().equals(userId)) {
            throw new IllegalArgumentException("그룹장은 그룹에서 나갈 수 없습니다. 그룹을 삭제하세요.");
        }
        repo.removeMember(groupId, userId);
    }

    public void kickMember(Long groupId, String targetUserId, String requesterId) {
        GroupTrip trip = repo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));
        if (!trip.getHostUserId().equals(requesterId)) {
            throw new IllegalArgumentException("그룹장만 멤버를 추방할 수 있습니다.");
        }
        if (targetUserId.equals(requesterId)) {
            throw new IllegalArgumentException("본인을 추방할 수 없습니다.");
        }
        repo.removeMember(groupId, targetUserId);
    }

    public GroupPlace addPlace(Long groupId, String userId, String userName, GroupPlace place) {
        if (!repo.isMember(groupId, userId)) {
            throw new IllegalArgumentException("그룹 멤버만 장소를 추가할 수 있습니다.");
        }
        place.setAddedBy(userId);
        place.setAddedByName(userName);
        return repo.addPlace(groupId, place);
    }

    public void removePlace(Long groupId, Long placeId, String userId) {
        if (!repo.isMember(groupId, userId)) {
            throw new IllegalArgumentException("그룹 멤버만 장소를 삭제할 수 있습니다.");
        }
        repo.removePlace(placeId, groupId);
    }
}
