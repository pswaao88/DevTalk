package com.devtalk.devtalk.infra.persistence;

import com.devtalk.devtalk.domain.session.Session;
import com.devtalk.devtalk.domain.session.SessionRepository;
import com.devtalk.devtalk.domain.session.SessionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSessionRepository implements SessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Session save(Session session){
        String checkSessionSql = "SELECT COUNT(*) FROM session WHERE session_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSessionSql, Integer.class, session.getSessionId());
        // update
        if(count != null && count > 0){
            String sql = "UPDATE session SET title = ?, status = ?, description = ?, ai_summary = ?, last_updated_at = ? WHERE session_id = ?";
            jdbcTemplate.update(sql,
                session.getTitle(),
                session.getStatus().name(), // Enum은 .name()으로 문자열 변환
                session.getDescription(),
                session.getAiSummary(),
                session.getLastUpdatedAt(),
                session.getSessionId() // WHERE 절의 물음표 값
            );
            return session;
        }
        // insert
        String sql = "INSERT INTO session (session_id, title, status, description, ai_summary, last_summarized_index, created_at, last_updated_at) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
            session.getSessionId(),
            session.getTitle(),
            session.getStatus().name(), // Enum은 .name()으로 문자열 변환
            session.getDescription(),
            session.getAiSummary(),
            0,
            session.getCreatedAt(),
            session.getLastUpdatedAt()
        );
        return session;
    }

    @Override
    public Optional<Session> findById(String sessionId){
        String sql = "SELECT * FROM session WHERE session_id = ?";
        List<Session> result = jdbcTemplate.query(sql, sessionRowMapper, sessionId);
        return result.stream().findFirst();
    }

    @Override
    public boolean existsById(String sessionId){
        String sql = "SELECT COUNT(*) FROM session WHERE session_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, sessionId);
        return count != null && count > 0;
    }

    @Override
    public void deleteById(String sessionId){
        String sql = "DELETE FROM session WHERE session_id = ?";
        jdbcTemplate.update(sql, sessionId);
    }
    @Override
    public List<Session> findAll(){
        String sql = "SELECT * FROM session ORDER BY last_updated_at DESC";
        return jdbcTemplate.query(sql, sessionRowMapper);
    }
    private final RowMapper<Session> sessionRowMapper = (rs, rowNum) -> new Session(
        rs.getString("session_id"),
        rs.getString("title"),
        SessionStatus.valueOf(rs.getString("status")),
        rs.getString("description"),
        rs.getString("ai_summary"),
        rs.getTimestamp("created_at").toLocalDateTime(),
        rs.getTimestamp("last_updated_at").toLocalDateTime()
    );
}
