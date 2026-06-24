package com.enjoytrip.repository;

import com.enjoytrip.model.Attraction;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AttractionRepository {

    private final DataSource dataSource;

    public AttractionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Attraction> search(Integer sidoCode, Integer gugunCode, Integer contentTypeId, String keyword) {
        StringBuilder sql = new StringBuilder(
                "SELECT a.content_id, a.title, a.content_type_id, a.area_code, a.si_gun_gu_code, " +
                "a.first_image1, a.first_image2, a.latitude, a.longitude, a.addr1, a.overview " +
                "FROM attractions a WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();
        if (sidoCode != null) {
            sql.append(" AND a.area_code = ?");
            params.add(sidoCode);
        }
        if (gugunCode != null) {
            sql.append(" AND a.si_gun_gu_code = ?");
            params.add(gugunCode);
        }
        if (contentTypeId != null) {
            sql.append(" AND a.content_type_id = ?");
            params.add(contentTypeId);
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (a.title LIKE ? OR a.addr1 LIKE ?)");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
        }
        sql.append(" LIMIT 20");
        
        List<Attraction> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("관광지 조회 실패", e);
        }
        return results;
    }

    private Attraction mapRow(ResultSet rs) throws SQLException {
        Attraction a = new Attraction();
        a.setContentId(String.valueOf(rs.getInt("content_id")));
        a.setTitle(rs.getString("title"));
        a.setContentTypeId(String.valueOf(rs.getInt("content_type_id")));
        a.setAreaCode(String.valueOf(rs.getInt("area_code")));
        a.setAddress(rs.getString("addr1") == null ? "" : rs.getString("addr1"));
        a.setOverview(rs.getString("overview") == null ? "" : rs.getString("overview"));

        String img1 = rs.getString("first_image1");
        String img2 = rs.getString("first_image2");
        a.setImageUrl(img1 != null && !img1.isBlank() ? img1 : (img2 != null ? img2 : ""));

        double lat = rs.getDouble("latitude");
        double lng = rs.getDouble("longitude");
        a.setLat(lat);
        a.setLng(lng);
        return a;
    }
}
