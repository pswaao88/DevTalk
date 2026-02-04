package com.devtalk.devtalk.infra.persistence;

import com.devtalk.devtalk.domain.llm.context.SessionSummaryStore;
import com.devtalk.devtalk.domain.llm.context.SummaryState;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSessionSummaryStore implements SessionSummaryStore {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSessionSummaryStore(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public SummaryState getState(String sessionId){
        String sql = "SELECT ai_summary, last_summarized_index FROM session WHERE session_id = ?";
        List<SummaryState> result = jdbcTemplate.query(sql, summaryStateRowMapper, sessionId);
        return result.stream()
            .findFirst()
            .orElse(new SummaryState("", 0));    }

    @Override
    public void putState(String sessionId, SummaryState state){
        String sql = "UPDATE session SET ai_summary = ?, last_summarized_index = ? WHERE session_id = ?";
        jdbcTemplate.update(sql, state.summaryText(), state.lastSummarizedIndex(), sessionId);
    }

    private final RowMapper<SummaryState> summaryStateRowMapper = (rs, rowNum) -> new SummaryState(
        rs.getString("ai_summary"),
        rs.getInt("last_summarized_index")
    );
}
