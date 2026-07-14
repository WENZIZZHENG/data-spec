package com.dataspec.standardcandidate;

import com.dataspec.common.mapper.ProjectFieldNameReservationMapper;
import com.dataspec.standardcandidate.entity.StandardCandidate;
import com.dataspec.standardcandidate.mapper.StandardCandidateMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TokenEvidenceCandidatePersistenceContractTest {

    @Test
    void mapperUsesParameterizedProjectCandidateAdvisoryLock() throws Exception {
        Method method = ProjectFieldNameReservationMapper.class.getMethod(
                "lock",
                Long.class,
                String.class);
        String sql = String.join("\n", method.getAnnotation(Select.class).value());

        assertThat(sql).contains(
                "pg_advisory_xact_lock",
                "hashtextextended",
                "#{fieldName}",
                "#{projectId}");
        assertThat(sql).doesNotContain("${");
    }

    @Test
    void mapperUsesParameterizedActiveCandidateChecks() throws Exception {
        Method direct = ProjectFieldNameReservationMapper.class.getMethod(
                "existsActiveCandidate",
                Long.class,
                String.class);
        Method fromCandidate = ProjectFieldNameReservationMapper.class.getMethod(
                "existsOtherActiveCandidate",
                Long.class,
                String.class,
                Long.class);

        String directSql = String.join("\n", direct.getAnnotation(Select.class).value());
        String fromCandidateSql = String.join("\n", fromCandidate.getAnnotation(Select.class).value());
        assertThat(directSql).contains(
                "ds_standard_candidate",
                "#{projectId}",
                "#{fieldName}",
                "PENDING",
                "POSTPONED",
                "is_deleted = false");
        assertThat(fromCandidateSql).contains("#{excludedCandidateId}");
        assertThat(directSql + fromCandidateSql).doesNotContain("${");
    }

    @Test
    void mapperUsesParameterizedOnConflictInsert() throws Exception {
        Method method = StandardCandidateMapper.class.getMethod(
                "insertTokenEvidenceIfAbsent",
                StandardCandidate.class);
        String sql = String.join("\n", method.getAnnotation(Insert.class).value());

        assertThat(sql).contains("ON CONFLICT DO NOTHING", "#{candidate.projectId}", "#{candidate.sourceRef}");
        assertThat(sql).doesNotContain("${");
    }

    @Test
    void migrationScopesUniqueIndexToNewSourceWithoutChangingHistoricalRows() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V32__add_token_evidence_candidate_idempotency.sql");
        String sql = Files.readString(migration, StandardCharsets.UTF_8);

        assertThat(sql).contains(
                "CREATE UNIQUE INDEX IF NOT EXISTS ux_standard_candidate_token_evidence_fact",
                "project_id, candidate_name, source_type, source_ref",
                "source_type = 'TOKEN_EVIDENCE'",
                "source_ref IS NOT NULL",
                "COMMENT ON INDEX");
        assertThat(sql).doesNotContain("UPDATE ds_standard_candidate", "DELETE FROM ds_standard_candidate");
    }
}
