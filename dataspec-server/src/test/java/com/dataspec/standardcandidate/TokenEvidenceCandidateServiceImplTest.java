package com.dataspec.standardcandidate;

import com.dataspec.businessglossary.model.GlossaryMatch;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.repository.ProjectFieldNameReservationRepository;
import com.dataspec.field.repository.FieldRepository;
import com.dataspec.querynormalization.model.QueryNormalizationResult;
import com.dataspec.querynormalization.model.QueryTokenEvidence;
import com.dataspec.querynormalization.model.QueryTokenKind;
import com.dataspec.querynormalization.model.QueryTokenResolution;
import com.dataspec.querynormalization.model.QueryTokenResolutionStatus;
import com.dataspec.querynormalization.service.QueryNormalizationService;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.model.ApiTokenPrincipal;
import com.dataspec.standardcandidate.entity.StandardCandidate;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidateApplyReq;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidateApplyResult;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidatePreview;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidatePreviewReq;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidatePreviewStatus;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidateSignalType;
import com.dataspec.standardcandidate.repository.TokenEvidenceCandidateRepository;
import com.dataspec.standardcandidate.service.impl.TokenEvidenceCandidateServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenEvidenceCandidateServiceImplTest {

    @Test
    void preview_rejectsUnauthorizedProjectBeforeReadingEvidenceOrCandidates() {
        Fixture fixture = fixture(unknownNormalization());
        DataSpecSecurityContext.set(new ApiTokenPrincipal("restricted", "tester", false, Set.of(2L)));
        try {
            BizException error = assertThrows(BizException.class, () -> fixture.service().preview(
                    request("field:orders.ord_amt", "ord")));

            assertThat(error.getCode()).isEqualTo(403);
            verifyNoInteractions(
                    fixture.candidateRepository(),
                    fixture.fieldRepository(),
                    fixture.fieldNameReservationRepository());
            verify(fixture.normalizationService(), never()).normalize(anyLong(), anyString());
        } finally {
            DataSpecSecurityContext.clear();
        }
    }

    @Test
    void preview_returnsReadySecretSafeDryRunForUnknownTerm() {
        Fixture fixture = fixture(normalization(resolution(
                "ord",
                QueryTokenKind.WORD,
                QueryTokenResolutionStatus.UNRESOLVED,
                List.of())));
        TokenEvidenceCandidatePreviewReq req = request(
                "field:orders.ord_amt?token=raw-source-secret",
                "ord_amt Authorization: Bearer raw-bearer jdbc:postgresql://db.local/app");

        TokenEvidenceCandidatePreview preview = fixture.service().preview(req);

        assertThat(preview.status()).isEqualTo(TokenEvidenceCandidatePreviewStatus.READY);
        assertThat(preview.willWrite()).isFalse();
        assertThat(preview.dryRunToken()).startsWith("tec.");
        assertThat(preview.signals()).extracting(signal -> signal.signalType())
                .containsExactly(TokenEvidenceCandidateSignalType.UNKNOWN_TERM);
        assertThat(preview.inboxPayload().confidence()).isEqualTo(40);
        assertThat(preview.sourceRef()).contains("[REDACTED]");
        assertThat(preview.inboxPayload().evidenceJson()).contains("sourceTextHash", "UNKNOWN_TERM")
                .doesNotContain("raw-source-secret", "raw-bearer", "jdbc:postgresql");
        assertThat(preview.safety().readOnly()).isTrue();
        assertThat(preview.safety().containsRawSourceText()).isFalse();

        ArgumentCaptor<String> sourceText = ArgumentCaptor.forClass(String.class);
        verify(fixture.normalizationService()).normalize(eq(1L), sourceText.capture());
        assertThat(sourceText.getValue()).doesNotContain("raw-bearer", "jdbc:postgresql");
    }

    @Test
    void preview_keepsOnlyAmbiguousAbbreviationAndDisabledSignals() {
        GlossaryMatch abbreviation = glossaryMatch("ord", "ABBREVIATION", QueryTokenResolutionStatus.AMBIGUOUS);
        GlossaryMatch disabled = glossaryMatch("legacy", "DISABLED", QueryTokenResolutionStatus.DISABLED);
        Fixture fixture = fixture(normalization(
                resolution("ord", QueryTokenKind.ACRONYM, QueryTokenResolutionStatus.AMBIGUOUS, List.of(abbreviation)),
                resolution("legacy", QueryTokenKind.WORD, QueryTokenResolutionStatus.DISABLED, List.of(disabled)),
                resolution("2", QueryTokenKind.NUMBER, QueryTokenResolutionStatus.UNRESOLVED, List.of()),
                resolution("kg", QueryTokenKind.UNIT, QueryTokenResolutionStatus.UNRESOLVED, List.of()),
                resolution("redacted", QueryTokenKind.WORD, QueryTokenResolutionStatus.UNRESOLVED, List.of()),
                resolution("amount", QueryTokenKind.WORD, QueryTokenResolutionStatus.RESOLVED, List.of())));

        TokenEvidenceCandidatePreview preview = fixture.service().preview(request("field:orders.ord_amt", "ord legacy 2 kg"));

        assertThat(preview.signals()).extracting(signal -> signal.signalType()).containsExactly(
                TokenEvidenceCandidateSignalType.AMBIGUOUS_ABBREVIATION,
                TokenEvidenceCandidateSignalType.DISABLED_NAMING);
        assertThat(preview.inboxPayload().confidence()).isEqualTo(10);
    }

    @Test
    void preview_reportsNoSignalFieldDuplicateExactDuplicateAndNameConflict() {
        Fixture noSignal = fixture(normalization(resolution(
                "order",
                QueryTokenKind.WORD,
                QueryTokenResolutionStatus.RESOLVED,
                List.of())));
        assertThat(noSignal.service().preview(request("field:orders.order_id", "order")).status())
                .isEqualTo(TokenEvidenceCandidatePreviewStatus.NO_ACTIONABLE_SIGNAL);

        Fixture fieldExists = fixture(unknownNormalization());
        when(fieldExists.fieldRepository().existsByNameInProject("ord_amt", 1L)).thenReturn(true);
        assertThat(fieldExists.service().preview(request("field:orders.ord_amt", "ord")).status())
                .isEqualTo(TokenEvidenceCandidatePreviewStatus.STANDARD_EXISTS);

        Fixture exact = fixture(unknownNormalization());
        StandardCandidate exactCandidate = candidate(21L, "field:orders.ord_amt");
        when(exact.candidateRepository().findByFactKey(1L, "ord_amt", "TOKEN_EVIDENCE", "field:orders.ord_amt"))
                .thenReturn(Optional.of(exactCandidate));
        TokenEvidenceCandidatePreview exactPreview = exact.service().preview(request("field:orders.ord_amt", "ord"));
        assertThat(exactPreview.status()).isEqualTo(TokenEvidenceCandidatePreviewStatus.EXACT_DUPLICATE);
        assertThat(exactPreview.duplicateCandidateId()).isEqualTo(21L);
        assertThat(exactPreview.dryRunToken()).isNull();

        Fixture conflict = fixture(unknownNormalization());
        when(conflict.candidateRepository().findActiveByName(1L, "ord_amt"))
                .thenReturn(Optional.of(candidate(22L, "manual:ord_amt")));
        TokenEvidenceCandidatePreview conflictPreview = conflict.service().preview(request("field:orders.ord_amt", "ord"));
        assertThat(conflictPreview.status()).isEqualTo(TokenEvidenceCandidatePreviewStatus.NAME_CONFLICT);
        assertThat(conflictPreview.duplicateCandidateId()).isEqualTo(22L);
    }

    @Test
    void apply_requiresConfirmationAndMatchingEvidence() {
        Fixture fixture = fixture(unknownNormalization());
        TokenEvidenceCandidatePreviewReq input = request("field:orders.ord_amt", "ord");
        TokenEvidenceCandidatePreview preview = fixture.service().preview(input);

        assertThrows(BizException.class, () -> fixture.service().apply(
                new TokenEvidenceCandidateApplyReq(input, preview.dryRunToken(), false)));
        assertThrows(BizException.class, () -> fixture.service().apply(
                new TokenEvidenceCandidateApplyReq(input, "tec.invalid.signature", true)));

        when(fixture.normalizationService().normalize(anyLong(), anyString())).thenReturn(normalization(resolution(
                "different",
                QueryTokenKind.WORD,
                QueryTokenResolutionStatus.UNRESOLVED,
                List.of())));
        BizException drift = assertThrows(BizException.class, () -> fixture.service().apply(
                new TokenEvidenceCandidateApplyReq(input, preview.dryRunToken(), true)));
        assertThat(drift.getMessage()).contains("已变化");
        verify(fixture.candidateRepository(), never()).insertIfAbsent(any());
    }

    @Test
    void apply_rejectsCandidateMetadataDriftBoundToPreviewToken() {
        TokenEvidenceCandidatePreviewReq input = request("field:orders.ord_amt", "ord");
        List<TokenEvidenceCandidatePreviewReq> driftedInputs = List.of(
                new TokenEvidenceCandidatePreviewReq(
                        input.projectId(), input.candidateName(), input.displayName(), "varchar",
                        input.comment(), input.sourceRef(), input.sourceText()),
                new TokenEvidenceCandidatePreviewReq(
                        input.projectId(), input.candidateName(), input.displayName(), input.dataType(),
                        "已修改候选说明", input.sourceRef(), input.sourceText()),
                new TokenEvidenceCandidatePreviewReq(
                        input.projectId(), input.candidateName(), "已修改显示名", input.dataType(),
                        input.comment(), input.sourceRef(), input.sourceText()));

        for (TokenEvidenceCandidatePreviewReq drifted : driftedInputs) {
            Fixture fixture = fixture(unknownNormalization());
            TokenEvidenceCandidatePreview preview = fixture.service().preview(input);

            BizException error = assertThrows(BizException.class, () -> fixture.service().apply(
                    new TokenEvidenceCandidateApplyReq(drifted, preview.dryRunToken(), true)));

            assertThat(error.getMessage()).contains("已变化");
            verify(fixture.fieldNameReservationRepository(), never()).lock(anyLong(), anyString());
            verify(fixture.candidateRepository(), never()).insertIfAbsent(any());
        }
    }

    @Test
    void apply_createsOnceAndReturnsExistingCandidateOnRetry() {
        Fixture fixture = fixture(unknownNormalization());
        TokenEvidenceCandidatePreviewReq input = request("field:orders.ord_amt", "ord");
        TokenEvidenceCandidatePreview preview = fixture.service().preview(input);
        StandardCandidate persisted = candidate(30L, "field:orders.ord_amt");
        when(fixture.candidateRepository().findByFactKey(1L, "ord_amt", "TOKEN_EVIDENCE", "field:orders.ord_amt"))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.of(persisted));
        when(fixture.candidateRepository().insertIfAbsent(any(StandardCandidate.class))).thenReturn(1);

        TokenEvidenceCandidateApplyResult created = fixture.service().apply(
                new TokenEvidenceCandidateApplyReq(input, preview.dryRunToken(), true));

        assertThat(created.created()).isTrue();
        assertThat(created.deduplicated()).isFalse();
        assertThat(created.candidate().id()).isEqualTo(30L);
        ArgumentCaptor<StandardCandidate> inserted = ArgumentCaptor.forClass(StandardCandidate.class);
        verify(fixture.candidateRepository()).insertIfAbsent(inserted.capture());
        assertThat(inserted.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(inserted.getValue().getSourceType()).isEqualTo("TOKEN_EVIDENCE");
        verify(fixture.fieldNameReservationRepository()).lock(1L, "ord_amt");

        when(fixture.candidateRepository().findByFactKey(1L, "ord_amt", "TOKEN_EVIDENCE", "field:orders.ord_amt"))
                .thenReturn(Optional.of(persisted));
        TokenEvidenceCandidateApplyResult retry = fixture.service().apply(
                new TokenEvidenceCandidateApplyReq(input, preview.dryRunToken(), true));
        assertThat(retry.created()).isFalse();
        assertThat(retry.deduplicated()).isTrue();
        assertThat(retry.candidate().id()).isEqualTo(30L);
        assertThat(retry.candidate().sourceType()).isEqualTo("TOKEN_EVIDENCE");
    }

    @Test
    void apply_returnsConcurrentInsertAsDeduplicated() {
        Fixture fixture = fixture(unknownNormalization());
        TokenEvidenceCandidatePreviewReq input = request("field:orders.ord_amt", "ord");
        TokenEvidenceCandidatePreview preview = fixture.service().preview(input);
        StandardCandidate persisted = candidate(31L, "field:orders.ord_amt");
        when(fixture.candidateRepository().findByFactKey(1L, "ord_amt", "TOKEN_EVIDENCE", "field:orders.ord_amt"))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.of(persisted));
        when(fixture.candidateRepository().insertIfAbsent(any(StandardCandidate.class))).thenReturn(0);

        TokenEvidenceCandidateApplyResult result = fixture.service().apply(
                new TokenEvidenceCandidateApplyReq(input, preview.dryRunToken(), true));

        assertThat(result.created()).isFalse();
        assertThat(result.deduplicated()).isTrue();
        assertThat(result.candidate().id()).isEqualTo(31L);
    }

    private Fixture fixture(QueryNormalizationResult normalization) {
        TokenEvidenceCandidateRepository candidateRepository = mock(TokenEvidenceCandidateRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        ProjectFieldNameReservationRepository fieldNameReservationRepository =
                mock(ProjectFieldNameReservationRepository.class);
        QueryNormalizationService normalizationService = mock(QueryNormalizationService.class);
        when(normalizationService.normalize(anyLong(), anyString())).thenReturn(normalization);
        return new Fixture(
                new TokenEvidenceCandidateServiceImpl(
                        candidateRepository,
                        fieldRepository,
                        fieldNameReservationRepository,
                        normalizationService,
                        new ObjectMapper().findAndRegisterModules()),
                candidateRepository,
                fieldRepository,
                fieldNameReservationRepository,
                normalizationService);
    }

    private QueryNormalizationResult unknownNormalization() {
        return normalization(resolution(
                "ord",
                QueryTokenKind.WORD,
                QueryTokenResolutionStatus.UNRESOLVED,
                List.of()));
    }

    private QueryNormalizationResult normalization(QueryTokenResolution... resolutions) {
        List<QueryTokenResolution> values = List.of(resolutions);
        return new QueryNormalizationResult(
                values.stream().map(item -> item.evidence().normalizedToken()).reduce((left, right) -> left + " " + right).orElse(""),
                List.of(),
                values.stream().map(QueryTokenResolution::evidence).toList(),
                values.stream().flatMap(item -> item.glossaryMatches().stream()).toList(),
                values);
    }

    private QueryTokenResolution resolution(
            String token,
            QueryTokenKind kind,
            QueryTokenResolutionStatus status,
            List<GlossaryMatch> matches
    ) {
        QueryTokenEvidence evidence = new QueryTokenEvidence(
                token,
                token,
                kind,
                status,
                status == QueryTokenResolutionStatus.RESOLVED ? token : null,
                null,
                null,
                matches.stream().map(GlossaryMatch::glossaryId).toList(),
                "test evidence");
        return new QueryTokenResolution(evidence, matches);
    }

    private GlossaryMatch glossaryMatch(
            String token,
            String matchType,
            QueryTokenResolutionStatus status
    ) {
        return new GlossaryMatch(
                1L,
                token,
                token,
                matchType,
                0,
                null,
                null,
                Set.of(),
                status == QueryTokenResolutionStatus.DISABLED,
                "test",
                status,
                List.of(1L));
    }

    private TokenEvidenceCandidatePreviewReq request(String sourceRef, String sourceText) {
        return new TokenEvidenceCandidatePreviewReq(
                1L,
                "ord_amt",
                "订单金额",
                "decimal(18,2)",
                "订单金额草案",
                sourceRef,
                sourceText);
    }

    private StandardCandidate candidate(Long id, String sourceRef) {
        StandardCandidate candidate = new StandardCandidate();
        candidate.setId(id);
        candidate.setProjectId(1L);
        candidate.setCandidateName("ord_amt");
        candidate.setDataType("decimal(18,2)");
        candidate.setSourceType("TOKEN_EVIDENCE");
        candidate.setSourceRef(sourceRef);
        candidate.setStatus("PENDING");
        return candidate;
    }

    private record Fixture(
            TokenEvidenceCandidateServiceImpl service,
            TokenEvidenceCandidateRepository candidateRepository,
            FieldRepository fieldRepository,
            ProjectFieldNameReservationRepository fieldNameReservationRepository,
            QueryNormalizationService normalizationService
    ) {
    }
}
