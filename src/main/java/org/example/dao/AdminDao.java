package org.example.dao;

import org.example.config.DBConnection;
import org.example.domain.Admin;
import org.example.domain.AdminAuth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

public class AdminDao implements AdminRepository {

    @Override
    public Optional<Admin> findById(long id) {
        String sql = """
                SELECT id, name, login_id, created_at
                FROM admin
                WHERE id = ?
                """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapAdmin(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("관리자 ID 조회에 실패했습니다.", e);
        }
    }

    @Override
    public Optional<AdminAuth> findAuthByLoginId(String loginId) {
        String sql = """
                SELECT id, name, login_id, password_hash, created_at
                FROM admin
                WHERE login_id = ?
                """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, loginId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new AdminAuth(mapAdmin(resultSet), resultSet.getString("password_hash")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("관리자 로그인 ID 조회에 실패했습니다.", e);
        }
    }

    @Override
    public void updatePasswordHash(long adminId, String passwordHash) {
        String sql = """
                UPDATE admin
                SET password_hash = ?
                WHERE id = ?
                """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, passwordHash);
            statement.setLong(2, adminId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("관리자 비밀번호 해시 갱신에 실패했습니다.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("관리자 비밀번호 해시 갱신 중 DB 오류가 발생했습니다.", e);
        }
    }

    private Admin mapAdmin(ResultSet resultSet) throws SQLException {
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        return new Admin(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("login_id"),
                createdAt.toLocalDateTime()
        );
    }
}
