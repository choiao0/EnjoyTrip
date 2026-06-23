package com.enjoytrip.repository;

import com.enjoytrip.model.Gugun;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class GugunRepository {

    private final DataSource dataSource;

    public GugunRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Gugun> findBySidoCode(int sidoCode) {
        String sql = "SELECT gugun_code, gugun_name, sido_code FROM guguns WHERE sido_code = ? ORDER BY gugun_code";
        List<Gugun> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sidoCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Gugun gugun = new Gugun();
                    gugun.setGugunCode(rs.getInt("gugun_code"));
                    gugun.setGugunName(rs.getString("gugun_name"));
                    gugun.setSidoCode(rs.getInt("sido_code"));
                    list.add(gugun);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("구군 목록 조회 실패", e);
        }
        return list;
    }
}
