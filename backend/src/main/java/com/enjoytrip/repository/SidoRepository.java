package com.enjoytrip.repository;

import com.enjoytrip.model.Sido;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SidoRepository {

    private final DataSource dataSource;

    public SidoRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Sido> findAll() {
        String sql = "SELECT sido_code, sido_name FROM sidos ORDER BY sido_code";
        List<Sido> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Sido sido = new Sido();
                sido.setSidoCode(rs.getInt("sido_code"));
                sido.setSidoName(rs.getString("sido_name"));
                list.add(sido);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("시도 목록 조회 실패", e);
        }
        return list;
    }
}
