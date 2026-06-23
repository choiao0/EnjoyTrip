package com.enjoytrip.repository;

import com.enjoytrip.model.GroupMember;
import com.enjoytrip.model.GroupPlace;
import com.enjoytrip.model.GroupTrip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class GroupTripRepository {

    private static final Logger log = LoggerFactory.getLogger(GroupTripRepository.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DataSource dataSource;

    public GroupTripRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ── Group CRUD ────────────────────────────────────────────────

    public List<GroupTrip> findMyGroups(String userId) {
        String sql = """
                SELECT g.id, g.title, g.host_user_id, g.description, g.created_at, g.invite_code,
                       m2.user_name AS host_user_name
                FROM group_trips g
                JOIN group_members m  ON m.group_id = g.id AND m.user_id = ?
                JOIN group_members m2 ON m2.group_id = g.id AND m2.user_id = g.host_user_id
                ORDER BY g.created_at DESC
                """;
        List<GroupTrip> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapTrip(rs));
            }
        } catch (SQLException e) {
            log.error("그룹 목록 조회 실패 userId={}: {}", userId, e.getMessage(), e);
            throw new IllegalStateException("그룹 조회 실패", e);
        }
        return list;
    }

    public Optional<GroupTrip> findById(Long id) {
        String sql = """
                SELECT g.id, g.title, g.host_user_id, g.description, g.created_at, g.invite_code,
                       m.user_name AS host_user_name
                FROM group_trips g
                JOIN group_members m ON m.group_id = g.id AND m.user_id = g.host_user_id
                WHERE g.id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapTrip(rs));
            }
        } catch (SQLException e) {
            log.error("그룹 단건 조회 실패 id={}: {}", id, e.getMessage(), e);
            throw new IllegalStateException("그룹 조회 실패", e);
        }
        return Optional.empty();
    }

    public GroupTrip save(GroupTrip trip) {
        String sql = "INSERT INTO group_trips (title, host_user_id, description, created_at, invite_code) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, trip.getTitle());
            ps.setString(2, trip.getHostUserId());
            ps.setString(3, trip.getDescription());
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(5, trip.getInviteCode());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) trip.setId(keys.getLong(1));
            }
            log.info("그룹 생성 id={} title={}", trip.getId(), trip.getTitle());
        } catch (SQLException e) {
            log.error("그룹 생성 실패 title={}: {}", trip.getTitle(), e.getMessage(), e);
            throw new IllegalStateException("그룹 생성 실패", e);
        }
        trip.setCreatedAt(LocalDateTime.now().format(FMT));
        return trip;
    }

    public void delete(Long groupId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM group_trips WHERE id = ?")) {
            ps.setLong(1, groupId);
            int rows = ps.executeUpdate();
            log.info("그룹 삭제 id={} ({}건)", groupId, rows);
        } catch (SQLException e) {
            log.error("그룹 삭제 실패 id={}: {}", groupId, e.getMessage(), e);
            throw new IllegalStateException("그룹 삭제 실패", e);
        }
    }

    public void updateInviteCode(Long groupId, String code) {
        String sql = "UPDATE group_trips SET invite_code = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setLong(2, groupId);
            ps.executeUpdate();
            log.info("초대코드 업데이트 groupId={} code={}", groupId, code);
        } catch (SQLException e) {
            log.error("초대코드 업데이트 실패 groupId={}: {}", groupId, e.getMessage(), e);
            throw new IllegalStateException("초대코드 업데이트 실패", e);
        }
    }

    public Optional<GroupTrip> findByInviteCode(String code) {
        String sql = """
                SELECT g.id, g.title, g.host_user_id, g.description, g.created_at, g.invite_code,
                       m.user_name AS host_user_name
                FROM group_trips g
                JOIN group_members m ON m.group_id = g.id AND m.user_id = g.host_user_id
                WHERE g.invite_code = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapTrip(rs));
            }
        } catch (SQLException e) {
            log.error("초대코드 조회 실패 code={}: {}", code, e.getMessage(), e);
            throw new IllegalStateException("그룹 조회 실패", e);
        }
        return Optional.empty();
    }

    // ── Members ───────────────────────────────────────────────────

    public List<GroupMember> findMembers(Long groupId) {
        String sql = "SELECT id, group_id, user_id, user_name, joined_at FROM group_members WHERE group_id = ? ORDER BY joined_at";
        List<GroupMember> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapMember(rs));
            }
        } catch (SQLException e) {
            log.error("멤버 조회 실패 groupId={}: {}", groupId, e.getMessage(), e);
            throw new IllegalStateException("멤버 조회 실패", e);
        }
        return list;
    }

    public boolean isMember(Long groupId, String userId) {
        String sql = "SELECT COUNT(*) FROM group_members WHERE group_id = ? AND user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            log.error("멤버 확인 실패 groupId={} userId={}: {}", groupId, userId, e.getMessage(), e);
            throw new IllegalStateException("멤버 확인 실패", e);
        }
    }

    public GroupMember addMember(Long groupId, String userId, String userName) {
        String sql = "INSERT INTO group_members (group_id, user_id, user_name, joined_at) VALUES (?, ?, ?, ?)";
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setUserName(userName);
        LocalDateTime now = LocalDateTime.now();
        member.setJoinedAt(now.format(FMT));
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, groupId);
            ps.setString(2, userId);
            ps.setString(3, userName);
            ps.setTimestamp(4, Timestamp.valueOf(now));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) member.setId(keys.getLong(1));
            }
            log.info("멤버 추가 groupId={} userId={}", groupId, userId);
        } catch (SQLException e) {
            log.error("멤버 추가 실패 groupId={} userId={}: {}", groupId, userId, e.getMessage(), e);
            throw new IllegalStateException("멤버 추가 실패", e);
        }
        return member;
    }

    public void removeMember(Long groupId, String userId) {
        String sql = "DELETE FROM group_members WHERE group_id = ? AND user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            ps.setString(2, userId);
            int rows = ps.executeUpdate();
            log.info("멤버 삭제 groupId={} userId={} ({}건)", groupId, userId, rows);
        } catch (SQLException e) {
            log.error("멤버 삭제 실패 groupId={} userId={}: {}", groupId, userId, e.getMessage(), e);
            throw new IllegalStateException("멤버 삭제 실패", e);
        }
    }

    // ── Places ────────────────────────────────────────────────────

    public List<GroupPlace> findPlaces(Long groupId) {
        String sql = "SELECT id, group_id, content_id, title, lat, lng, added_by, added_by_name, added_at FROM group_places WHERE group_id = ? ORDER BY added_at";
        List<GroupPlace> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapPlace(rs));
            }
        } catch (SQLException e) {
            log.error("장소 조회 실패 groupId={}: {}", groupId, e.getMessage(), e);
            throw new IllegalStateException("장소 조회 실패", e);
        }
        return list;
    }

    public GroupPlace addPlace(Long groupId, GroupPlace place) {
        String sql = "INSERT INTO group_places (group_id, content_id, title, lat, lng, added_by, added_by_name, added_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now();
        place.setGroupId(groupId);
        place.setAddedAt(now.format(FMT));
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, groupId);
            ps.setString(2, place.getContentId());
            ps.setString(3, place.getTitle());
            ps.setDouble(4, place.getLat());
            ps.setDouble(5, place.getLng());
            ps.setString(6, place.getAddedBy());
            ps.setString(7, place.getAddedByName());
            ps.setTimestamp(8, Timestamp.valueOf(now));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) place.setId(keys.getLong(1));
            }
            log.info("장소 추가 groupId={} title={} by={}", groupId, place.getTitle(), place.getAddedBy());
        } catch (SQLException e) {
            log.error("장소 추가 실패 groupId={} title={}: {}", groupId, place.getTitle(), e.getMessage(), e);
            throw new IllegalStateException("장소 추가 실패", e);
        }
        return place;
    }

    public void removePlace(Long placeId, Long groupId) {
        String sql = "DELETE FROM group_places WHERE id = ? AND group_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, placeId);
            ps.setLong(2, groupId);
            int rows = ps.executeUpdate();
            log.info("장소 삭제 placeId={} groupId={} ({}건)", placeId, groupId, rows);
        } catch (SQLException e) {
            log.error("장소 삭제 실패 placeId={} groupId={}: {}", placeId, groupId, e.getMessage(), e);
            throw new IllegalStateException("장소 삭제 실패", e);
        }
    }

    // ── Mappers ───────────────────────────────────────────────────

    private GroupTrip mapTrip(ResultSet rs) throws SQLException {
        GroupTrip t = new GroupTrip();
        t.setId(rs.getLong("id"));
        t.setTitle(rs.getString("title"));
        t.setHostUserId(rs.getString("host_user_id"));
        t.setHostUserName(rs.getString("host_user_name"));
        t.setDescription(rs.getString("description"));
        t.setInviteCode(rs.getString("invite_code"));
        Timestamp ts = rs.getTimestamp("created_at");
        t.setCreatedAt(ts != null ? ts.toLocalDateTime().format(FMT) : "");
        return t;
    }

    private GroupMember mapMember(ResultSet rs) throws SQLException {
        GroupMember m = new GroupMember();
        m.setId(rs.getLong("id"));
        m.setGroupId(rs.getLong("group_id"));
        m.setUserId(rs.getString("user_id"));
        m.setUserName(rs.getString("user_name"));
        Timestamp ts = rs.getTimestamp("joined_at");
        m.setJoinedAt(ts != null ? ts.toLocalDateTime().format(FMT) : "");
        return m;
    }

    private GroupPlace mapPlace(ResultSet rs) throws SQLException {
        GroupPlace p = new GroupPlace();
        p.setId(rs.getLong("id"));
        p.setGroupId(rs.getLong("group_id"));
        p.setContentId(rs.getString("content_id"));
        p.setTitle(rs.getString("title"));
        p.setLat(rs.getDouble("lat"));
        p.setLng(rs.getDouble("lng"));
        p.setAddedBy(rs.getString("added_by"));
        p.setAddedByName(rs.getString("added_by_name"));
        Timestamp ts = rs.getTimestamp("added_at");
        p.setAddedAt(ts != null ? ts.toLocalDateTime().format(FMT) : "");
        return p;
    }
}
