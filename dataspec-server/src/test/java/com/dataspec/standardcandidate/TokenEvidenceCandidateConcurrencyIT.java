package com.dataspec.standardcandidate;

import com.dataspec.common.exception.BizException;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.model.ApiTokenPrincipal;
import com.dataspec.standardcandidate.entity.StandardCandidate;
import com.dataspec.standardcandidate.model.StandardCandidateCreateReq;
import com.dataspec.standardcandidate.model.StandardCandidateDecisionReq;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidateApplyReq;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidateApplyResult;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidatePreview;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidatePreviewReq;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidatePreviewStatus;
import com.dataspec.standardcandidate.service.StandardCandidateService;
import com.dataspec.standardcandidate.service.TokenEvidenceCandidateService;
import com.dataspec.starterkit.model.StarterKitDefinition;
import com.dataspec.starterkit.model.StarterKitFieldDefinition;
import com.dataspec.starterkit.service.StarterKitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 命名证据候选跨服务写入的真实 PostgreSQL 并发测试。
 *
 * <p>测试通过 Spring 事务代理调用生产 Service/Repository/Mapper，证明 token apply、通用候选创建、
 * 直接字段创建和候选采纳共用同一项目字段名预留锁。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TokenEvidenceCandidateConcurrencyIT {

    private static final String PASSWORD = "it-" + UUID.randomUUID();
    private static final String EXTERNAL_JDBC_URL = System.getenv("DATASPEC_CANDIDATE_IT_JDBC_URL");
    private static final String EXTERNAL_USERNAME = System.getenv("DATASPEC_CANDIDATE_IT_DB_USER");
    private static final String EXTERNAL_PASSWORD = System.getenv("DATASPEC_CANDIDATE_IT_DB_PASSWORD");
    private static final String EXTERNAL_OPT_IN = System.getenv("DATASPEC_CANDIDATE_IT_ALLOW_EXTERNAL_DATABASE");
    private static final String EXPECTED_DATABASE = "dataspec_candidate_it";

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:17-alpine"))
            .withDatabaseName(EXPECTED_DATABASE)
            .withUsername("dataspec_owner")
            .withPassword(PASSWORD);

    private static String jdbcUrl;
    private static String username;
    private static String password;

    @Autowired
    private TokenEvidenceCandidateService tokenEvidenceCandidateService;

    @Autowired
    private StandardCandidateService standardCandidateService;

    @Autowired
    private FieldService fieldService;

    @Autowired
    private StarterKitService starterKitService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ObjectMapper objectMapper;

    private long projectId;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) throws Exception {
        configureDatabaseTarget();
        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", () -> username);
        registry.add("spring.datasource.password", () -> password);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.baseline-on-migrate", () -> false);
    }

    @BeforeAll
    void createProject() {
        projectId = jdbcTemplate.queryForObject("""
                INSERT INTO ds_project(name, description, db_type)
                VALUES (?, 'transaction lock verification', 'POSTGRESQL')
                RETURNING id
                """, Long.class, "candidate-concurrency-it-" + UUID.randomUUID());
    }

    @AfterAll
    static void stopManagedContainer() {
        if (POSTGRES.isRunning()) {
            POSTGRES.stop();
        }
    }

    @Test
    void tokenApplyAndGenericCandidateCreate_shareProductionTransactionLock() throws Exception {
        String candidateName = uniqueName("candidate_race");
        TokenEvidenceCandidatePreviewReq input = previewInput(candidateName, "token:" + candidateName);
        TokenEvidenceCandidatePreview preview = withSecurity(() -> tokenEvidenceCandidateService.preview(input));
        assertThat(preview.status()).isEqualTo(TokenEvidenceCandidatePreviewStatus.READY);

        runApplyFirstRace(input, preview, () -> standardCandidateService.create(new StandardCandidateCreateReq(
                projectId,
                candidateName,
                "并发人工候选",
                "varchar",
                null,
                "MANUAL",
                "manual:" + candidateName,
                null,
                50)));

        assertThat(activeCandidateCount(candidateName)).isEqualTo(1);
        assertThat(fieldCount(candidateName)).isZero();
    }

    @Test
    void tokenApplyAndDirectFieldCreate_shareProductionTransactionLock() throws Exception {
        String candidateName = uniqueName("field_race");
        TokenEvidenceCandidatePreviewReq input = previewInput(candidateName, "token:" + candidateName);
        TokenEvidenceCandidatePreview preview = withSecurity(() -> tokenEvidenceCandidateService.preview(input));
        assertThat(preview.status()).isEqualTo(TokenEvidenceCandidatePreviewStatus.READY);

        runApplyFirstRace(input, preview, () -> fieldService.create(field(candidateName)));

        assertThat(activeCandidateCount(candidateName)).isEqualTo(1);
        assertThat(fieldCount(candidateName)).isZero();
    }

    @Test
    void tokenApplyAndStarterKitFieldBatch_shareProductionTransactionLock() throws Exception {
        StarterKitDefinition kit = starterKitService.listKits().stream()
                .filter(item -> item.fields() != null && !item.fields().isEmpty())
                .findFirst()
                .orElseThrow();
        StarterKitFieldDefinition seed = kit.fields().stream()
                .filter(item -> item.name() != null && !item.name().isBlank())
                .findFirst()
                .orElseThrow();
        String candidateName = seed.name();
        TokenEvidenceCandidatePreviewReq input = previewInput(candidateName, "token:starter-kit:" + candidateName);
        TokenEvidenceCandidatePreview preview = withSecurity(() -> tokenEvidenceCandidateService.preview(input));
        assertThat(preview.status()).isEqualTo(TokenEvidenceCandidatePreviewStatus.READY);

        runApplyFirstRace(input, preview, () ->
                starterKitService.applyKit(projectId, kit.key(), kit.version()));

        assertThat(activeCandidateCount(candidateName)).isEqualTo(1);
        assertThat(fieldCount(candidateName)).isZero();
    }

    @Test
    void directFieldCreateAndStarterKitBatch_refreshFieldSnapshotAfterLock() throws Exception {
        long isolatedProjectId = createProject("starter-kit-field-refresh");
        StarterKitDefinition kit = starterKitService.listKits().stream()
                .filter(item -> item.fields() != null && !item.fields().isEmpty())
                .findFirst()
                .orElseThrow();
        String fieldName = kit.fields().stream()
                .map(StarterKitFieldDefinition::name)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElseThrow();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch fieldInserted = new CountDownLatch(1);
        CountDownLatch releaseFieldCreate = new CountDownLatch(1);
        CountDownLatch batchStarted = new CountDownLatch(1);
        try {
            Future<Field> fieldFuture = executor.submit(() -> withSecurity(() ->
                    new TransactionTemplate(transactionManager).execute(status -> {
                        Field result = fieldService.create(field(isolatedProjectId, fieldName));
                        fieldInserted.countDown();
                        awaitRelease(releaseFieldCreate);
                        return result;
                    })));
            assertThat(fieldInserted.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> batchFuture = executor.submit(() -> withSecurity(() -> {
                batchStarted.countDown();
                return starterKitService.applyKit(isolatedProjectId, kit.key(), kit.version());
            }));
            assertThat(batchStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> batchFuture.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            releaseFieldCreate.countDown();

            assertThat(fieldFuture.get(5, TimeUnit.SECONDS).getName()).isEqualTo(fieldName);
            batchFuture.get(5, TimeUnit.SECONDS);
            assertThat(fieldCount(isolatedProjectId, fieldName)).isEqualTo(1);
        } finally {
            releaseFieldCreate.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void tokenApplyAndFieldUndoNameRestore_shareProductionTransactionLock() throws Exception {
        long isolatedProjectId = createProject("field-undo-name-reservation");
        String currentName = uniqueName("undo_current");
        String restoredName = uniqueName("undo_restored");
        Field created = withSecurity(() -> fieldService.create(field(isolatedProjectId, currentName)));
        Field snapshot = field(isolatedProjectId, restoredName);
        snapshot.setId(created.getId());
        Long logId = jdbcTemplate.queryForObject("""
                INSERT INTO ds_standard_change_log(
                    project_id, target_type, target_id, action, before_json, after_json)
                VALUES (?, 'field', ?, 'update', ?, ?)
                RETURNING id
                """, Long.class,
                isolatedProjectId,
                created.getId(),
                objectMapper.writeValueAsString(snapshot),
                objectMapper.writeValueAsString(created));
        TokenEvidenceCandidatePreviewReq input = previewInput(
                isolatedProjectId,
                restoredName,
                "token:undo:" + restoredName);
        TokenEvidenceCandidatePreview preview = withSecurity(() -> tokenEvidenceCandidateService.preview(input));
        assertThat(preview.status()).isEqualTo(TokenEvidenceCandidatePreviewStatus.READY);

        runApplyFirstRace(input, preview, () -> fieldService.undoFieldChange(created.getId(), logId));

        assertThat(activeCandidateCount(isolatedProjectId, restoredName)).isEqualTo(1);
        assertThat(fieldCount(isolatedProjectId, restoredName)).isZero();
        assertThat(fieldCount(isolatedProjectId, currentName)).isEqualTo(1);
    }

    @Test
    void candidateAcceptAndTokenApply_shareProductionTransactionLock() throws Exception {
        String candidateName = uniqueName("accept_race");
        TokenEvidenceCandidatePreviewReq input = previewInput(candidateName, "token:" + candidateName);
        TokenEvidenceCandidatePreview preview = withSecurity(() -> tokenEvidenceCandidateService.preview(input));
        assertThat(preview.status()).isEqualTo(TokenEvidenceCandidatePreviewStatus.READY);
        StandardCandidate manual = withSecurity(() -> standardCandidateService.create(new StandardCandidateCreateReq(
                projectId,
                candidateName,
                "待采纳候选",
                "varchar",
                null,
                "MANUAL",
                "manual:" + candidateName,
                null,
                60)));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch accepted = new CountDownLatch(1);
        CountDownLatch releaseAccept = new CountDownLatch(1);
        CountDownLatch applyStarted = new CountDownLatch(1);
        try {
            Future<StandardCandidate> acceptFuture = executor.submit(() -> withSecurity(() ->
                    new TransactionTemplate(transactionManager).execute(status -> {
                        StandardCandidate result = standardCandidateService.accept(
                                manual.getId(),
                                new StandardCandidateDecisionReq("并发采纳验证"));
                        accepted.countDown();
                        awaitRelease(releaseAccept);
                        return result;
                    })));
            assertThat(accepted.await(5, TimeUnit.SECONDS)).isTrue();

            Future<TokenEvidenceCandidateApplyResult> applyFuture = executor.submit(() -> withSecurity(() -> {
                applyStarted.countDown();
                return tokenEvidenceCandidateService.apply(
                        new TokenEvidenceCandidateApplyReq(input, preview.dryRunToken(), true));
            }));
            assertThat(applyStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> applyFuture.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            releaseAccept.countDown();

            assertThat(acceptFuture.get(5, TimeUnit.SECONDS).getStatus()).isEqualTo("ACCEPTED");
            assertFutureRejectedByBusinessRule(applyFuture);
            assertThat(fieldCount(candidateName)).isEqualTo(1);
            assertThat(tokenEvidenceCandidateCount(candidateName)).isZero();
        } finally {
            releaseAccept.countDown();
            executor.shutdownNow();
        }
    }

    private void runApplyFirstRace(
            TokenEvidenceCandidatePreviewReq input,
            TokenEvidenceCandidatePreview preview,
            Callable<?> competingWrite
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch applied = new CountDownLatch(1);
        CountDownLatch releaseApply = new CountDownLatch(1);
        CountDownLatch competingStarted = new CountDownLatch(1);
        try {
            Future<TokenEvidenceCandidateApplyResult> applyFuture = executor.submit(() -> withSecurity(() ->
                    new TransactionTemplate(transactionManager).execute(status -> {
                        TokenEvidenceCandidateApplyResult result = tokenEvidenceCandidateService.apply(
                                new TokenEvidenceCandidateApplyReq(input, preview.dryRunToken(), true));
                        applied.countDown();
                        awaitRelease(releaseApply);
                        return result;
                    })));
            assertThat(applied.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> competingFuture = executor.submit(() -> withSecurity(() -> {
                competingStarted.countDown();
                return competingWrite.call();
            }));
            assertThat(competingStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> competingFuture.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            releaseApply.countDown();

            assertThat(applyFuture.get(5, TimeUnit.SECONDS).created()).isTrue();
            assertFutureRejectedByBusinessRule(competingFuture);
        } finally {
            releaseApply.countDown();
            executor.shutdownNow();
        }
    }

    private void assertFutureRejectedByBusinessRule(Future<?> future) {
        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(BizException.class);
    }

    private TokenEvidenceCandidatePreviewReq previewInput(String candidateName, String sourceRef) {
        return previewInput(projectId, candidateName, sourceRef);
    }

    private TokenEvidenceCandidatePreviewReq previewInput(
            long targetProjectId,
            String candidateName,
            String sourceRef
    ) {
        return new TokenEvidenceCandidatePreviewReq(
                targetProjectId,
                candidateName,
                "并发命名证据",
                "varchar",
                "生产服务并发验证",
                sourceRef,
                "unresolvedterm " + candidateName);
    }

    private Field field(String candidateName) {
        return field(projectId, candidateName);
    }

    private Field field(long targetProjectId, String candidateName) {
        Field field = new Field();
        field.setProjectId(targetProjectId);
        field.setName(candidateName);
        field.setDisplayName("并发直接字段");
        field.setDataType("varchar");
        return field;
    }

    private long activeCandidateCount(String candidateName) {
        return activeCandidateCount(projectId, candidateName);
    }

    private long activeCandidateCount(long targetProjectId, String candidateName) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM ds_standard_candidate
                WHERE project_id = ?
                  AND candidate_name = ?
                  AND status IN ('PENDING', 'POSTPONED')
                  AND is_deleted = false
                """, Long.class, targetProjectId, candidateName);
        return count == null ? 0L : count;
    }

    private long tokenEvidenceCandidateCount(String candidateName) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM ds_standard_candidate
                WHERE project_id = ?
                  AND candidate_name = ?
                  AND source_type = 'TOKEN_EVIDENCE'
                  AND is_deleted = false
                """, Long.class, projectId, candidateName);
        return count == null ? 0L : count;
    }

    private long fieldCount(String fieldName) {
        return fieldCount(projectId, fieldName);
    }

    private long fieldCount(long targetProjectId, String fieldName) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM ds_field
                WHERE project_id = ?
                  AND name = ?
                  AND is_deleted = false
                """, Long.class, targetProjectId, fieldName);
        return count == null ? 0L : count;
    }

    private long createProject(String prefix) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO ds_project(name, description, db_type)
                VALUES (?, 'transaction lock verification', 'POSTGRESQL')
                RETURNING id
                """, Long.class, prefix + "-" + UUID.randomUUID());
    }

    private String uniqueName(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private <T> T withSecurity(Callable<T> action) throws Exception {
        DataSpecSecurityContext.set(ApiTokenPrincipal.local());
        try {
            return action.call();
        } finally {
            DataSpecSecurityContext.clear();
        }
    }

    private static void awaitRelease(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("未收到事务释放信号");
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待事务释放时被中断", error);
        }
    }

    private static synchronized void configureDatabaseTarget() throws Exception {
        if (jdbcUrl != null) {
            return;
        }
        if (EXTERNAL_JDBC_URL == null || EXTERNAL_JDBC_URL.isBlank()) {
            POSTGRES.start();
            jdbcUrl = POSTGRES.getJdbcUrl();
            username = POSTGRES.getUsername();
            password = POSTGRES.getPassword();
            return;
        }
        jdbcUrl = EXTERNAL_JDBC_URL;
        username = requireExternalValue(EXTERNAL_USERNAME, "DATASPEC_CANDIDATE_IT_DB_USER");
        password = requireExternalValue(EXTERNAL_PASSWORD, "DATASPEC_CANDIDATE_IT_DB_PASSWORD");
        validateExternalDatabase();
    }

    /** 外部入口只允许显式授权且没有任何用户对象的一次性数据库。 */
    private static void validateExternalDatabase() throws Exception {
        if (!Boolean.parseBoolean(EXTERNAL_OPT_IN)) {
            throw new IllegalStateException(
                    "DATASPEC_CANDIDATE_IT_ALLOW_EXTERNAL_DATABASE=true is required for the external integration database");
        }
        try (Connection connection = connection()) {
            assertExternalDatabaseIsOwnedAndEmpty(connection, username);
        }
    }

    static void assertExternalDatabaseIsOwnedAndEmpty(Connection connection, String expectedUsername) throws Exception {
        assertThat(connection.getCatalog()).isEqualTo(EXPECTED_DATABASE);
        assertThat(connection.getSchema()).isEqualTo("public");
        try (Statement statement = connection.createStatement()) {
            try (ResultSet owner = statement.executeQuery("""
                    SELECT pg_get_userbyid(datdba), current_user
                    FROM pg_database
                    WHERE datname = current_database()
                    """)) {
                assertThat(owner.next()).isTrue();
                assertThat(owner.getString(1))
                        .as("external integration database owner must match the configured user")
                        .isEqualTo(expectedUsername);
                assertThat(owner.getString(2))
                        .as("external integration database current user must own the database")
                        .isEqualTo(expectedUsername);
            }
            try (ResultSet resultSet = statement.executeQuery(externalDatabaseUserObjectsSql())) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt(1))
                        .as("external integration database must contain no user objects")
                        .isZero();
            }
        }
    }

    static String externalDatabaseUserObjectsSql() {
        return """
                SELECT COUNT(*)
                FROM (
                    SELECT c.oid FROM pg_class c
                    JOIN pg_namespace n ON n.oid = c.relnamespace
                    WHERE n.nspname = 'public'
                    UNION ALL
                    SELECT p.oid FROM pg_proc p
                    JOIN pg_namespace n ON n.oid = p.pronamespace
                    WHERE n.nspname = 'public'
                    UNION ALL
                    SELECT t.oid FROM pg_type t
                    JOIN pg_namespace n ON n.oid = t.typnamespace
                    WHERE n.nspname = 'public'
                    UNION ALL
                    SELECT o.oid FROM pg_operator o
                    JOIN pg_namespace n ON n.oid = o.oprnamespace
                    WHERE n.nspname = 'public'
                    UNION ALL
                    SELECT c.oid FROM pg_collation c
                    JOIN pg_namespace n ON n.oid = c.collnamespace
                    WHERE n.nspname = 'public'
                    UNION ALL
                    SELECT c.oid FROM pg_conversion c
                    JOIN pg_namespace n ON n.oid = c.connamespace
                    WHERE n.nspname = 'public'
                    UNION ALL
                    SELECT s.oid FROM pg_statistic_ext s
                    JOIN pg_namespace n ON n.oid = s.stxnamespace
                    WHERE n.nspname = 'public'
                    UNION ALL
                    SELECT c.oid FROM pg_ts_config c
                    JOIN pg_namespace n ON n.oid = c.cfgnamespace
                    WHERE n.nspname = 'public'
                    UNION ALL
                    SELECT d.oid FROM pg_ts_dict d
                    JOIN pg_namespace n ON n.oid = d.dictnamespace
                    WHERE n.nspname = 'public'
                    UNION ALL
                    SELECT p.oid FROM pg_ts_parser p
                    JOIN pg_namespace n ON n.oid = p.prsnamespace
                    WHERE n.nspname = 'public'
                    UNION ALL
                    SELECT t.oid FROM pg_ts_template t
                    JOIN pg_namespace n ON n.oid = t.tmplnamespace
                    WHERE n.nspname = 'public'
                    UNION ALL
                    SELECT n.oid FROM pg_namespace n
                    WHERE n.nspname <> 'public'
                      AND n.nspname <> 'information_schema'
                      AND n.nspname NOT LIKE 'pg_%'
                    UNION ALL
                    SELECT e.oid FROM pg_extension e
                    WHERE e.extname <> 'plpgsql'
                    UNION ALL
                    SELECT p.oid FROM pg_publication p
                    UNION ALL
                    SELECT s.oid FROM pg_subscription s
                    WHERE s.subdbid = (SELECT oid FROM pg_database WHERE datname = current_database())
                    UNION ALL
                    SELECT e.oid FROM pg_event_trigger e
                    UNION ALL
                    SELECT f.oid FROM pg_foreign_data_wrapper f
                    UNION ALL
                    SELECT s.oid FROM pg_foreign_server s
                    UNION ALL
                    SELECT m.oid FROM pg_user_mapping m
                    UNION ALL
                    SELECT l.oid FROM pg_largeobject_metadata l
                    UNION ALL
                    SELECT d.oid FROM pg_default_acl d
                    UNION ALL
                    SELECT l.oid FROM pg_language l
                    WHERE l.lanname NOT IN ('internal', 'c', 'sql', 'plpgsql')
                ) user_objects
                """;
    }

    private static String requireExternalValue(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(variableName + " is required for the external integration database");
        }
        return value;
    }

    private static Connection connection() throws Exception {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}
