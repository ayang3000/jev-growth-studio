package com.example.jevseo.repository;

import com.example.jevseo.domain.OptimizationReport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OptimizationRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public OptimizationRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(OptimizationReport report) {
        jdbcTemplate.update("""
                        insert into optimization_report(id, created_at, channel, product_name, status, payload)
                        values (?, ?, ?, ?, ?, ?)
                        """,
                report.id(), report.createdAt(), report.request().channel().name(),
                report.request().productName(), report.status(), toJson(report));
    }

    public Optional<OptimizationReport> findById(UUID id) {
        return jdbcTemplate.query("select payload from optimization_report where id = ?",
                        (rs, rowNum) -> fromJson(rs.getString("payload")), id)
                .stream().findFirst();
    }

    public List<OptimizationReport> findRecent(int limit) {
        return jdbcTemplate.query("select payload from optimization_report order by created_at desc limit ?",
                (rs, rowNum) -> fromJson(rs.getString("payload")), limit);
    }

    public void saveFeedback(UUID reportId, String metric, double baseline, double observed, String notes) {
        jdbcTemplate.update("""
                        insert into optimization_feedback(id, report_id, created_at, metric, baseline, observed, notes)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """, UUID.randomUUID(), reportId, Instant.now(), metric, baseline, observed, notes);
    }

    private String toJson(OptimizationReport report) {
        try {
            return objectMapper.writeValueAsString(report);
        }
        catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize optimization report", exception);
        }
    }

    private OptimizationReport fromJson(String payload) {
        try {
            return objectMapper.readValue(payload, OptimizationReport.class);
        }
        catch (JacksonException exception) {
            throw new IllegalStateException("Could not deserialize optimization report", exception);
        }
    }
}
