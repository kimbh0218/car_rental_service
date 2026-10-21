package org.example.dao;

import org.example.config.DBConnection;
import org.example.domain.Member;
import org.example.domain.MemberAuth;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MemberDao {

    public List<Member> findAll() {
        String sql = """
                SELECT id, name, license_status, login_id
                FROM member
                ORDER BY id
                """;
        List<Member> members = new ArrayList<>();
        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery();
        ) {
            // 조회 결과 처리
            while (resultSet.next()) {
                Member member = new Member(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getBoolean("license_status"),
                        resultSet.getString("login_id")
                );
                members.add(member);
            }
        } catch (Exception e) {
            throw new RuntimeException("회원 목록 조회에 실패했습니다.", e);
        }
        return members;
    }

    public int save(Member member, String passwordHash) {
        String sql = """
                INSERT INTO member (name, license_status, login_id, password_hash)
                VALUES (?, ?, ?, ?)
                """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS
                        )
        ) {
            statement.setString(1, member.getName());
            statement.setBoolean(2, member.isLicenseStatus());
            statement.setString(3, member.getLoginId());
            statement.setString(4, passwordHash);

            int affectedRows = statement.executeUpdate();

            if (affectedRows != 1) {
                throw new IllegalStateException("회원 등록에 실패했습니다.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            } throw new SQLException("생성된 member_id를 받지 못했습니다.");
        } catch (SQLException e) {
            if (isDuplicateLoginId(e)) {
                throw new DuplicateLoginIdException(e);
            }
            throw new RuntimeException("회원 등록 중 DB 오류가 발생했습니다.", e);
        }
    }

    public void updatePasswordHash(long memberId, String passwordHash) {
        String sql = """
                UPDATE member
                SET password_hash = ?
                WHERE id = ?
                """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, passwordHash);
            statement.setLong(2, memberId);

            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("비밀번호 해시 갱신에 실패했습니다.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("비밀번호 해시 갱신 중 DB 오류가 발생했습니다.", e);
        }
    }

    public int updateLicenseStatus(long id, boolean license_status) {
        String sql = """
                UPDATE member
                SET license_status = ?
                WHERE id = ?
                """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
        ) {
            // 첫번째 ?에 licens_status 넣기
            statement.setBoolean(1, license_status);
            // 두번째 ?에 id 넣기
            statement.setLong(2, id);
            // UPDATE 실행 결과를 int로 받아 반환
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("라이선스 상태 변경에 실패했습니다.", e);
        }
    }

    public int deleteById(long id) {
        String sql = """
                DELETE FROM member             
                WHERE id = ?               
                """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
        ) {
            statement.setLong(1, id);
            return statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("회원 삭제에 실패했습니다.", e);
        }
    }


    public Optional<Member> findById(long id) {
        String sql = """
            SELECT id, name, license_status, login_id
            FROM member
            WHERE id = ?
            """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Member member = new Member(
                            resultSet.getLong("id"),
                            resultSet.getString("name"),
                            resultSet.getBoolean("license_status"),
                            resultSet.getString("login_id")
                    );

                    return Optional.of(member);
                }
            }

            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("회원 ID 조회에 실패했습니다.", e);
        }
    }

    public Optional<Member> findById(Connection connection, long id) {
        String sql = """
                SELECT id, name, license_status, login_id
                FROM member
                WHERE id = ?
                """;

        try (
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ){
            preparedStatement.setLong(1,id);

            try (ResultSet resultSet = preparedStatement.executeQuery()){
                if (resultSet.next()){
                    Member member = new Member (
                            resultSet.getLong("id"),
                            resultSet.getString("name"),
                            resultSet.getBoolean("license_status"),
                            resultSet.getString("login_id")
                    );

                    return Optional.of(member);
                }
                return Optional.empty();
            }
        }catch (SQLException e){
            throw new RuntimeException("회원 ID 조회에 실패했습니다.", e);
        }
    }

    public Optional<MemberAuth> findAuthByLoginId(String loginId) {
        String sql = """
                SELECT id, name, license_status, login_id, password_hash
                FROM member
                WHERE login_id = ?
                """;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, loginId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Member member = new Member(
                            resultSet.getLong("id"),
                            resultSet.getString("name"),
                            resultSet.getBoolean("license_status"),
                            resultSet.getString("login_id")
                    );
                    return Optional.of(new MemberAuth(
                            member,
                            resultSet.getString("password_hash")
                    ));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("로그인 ID 조회에 실패했습니다.", e);
        }
    }

    private boolean isDuplicateLoginId(SQLException exception) {
        return "23000".equals(exception.getSQLState())
                && exception.getErrorCode() == 1062;
    }
}
