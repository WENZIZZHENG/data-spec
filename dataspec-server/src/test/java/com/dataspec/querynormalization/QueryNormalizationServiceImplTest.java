package com.dataspec.querynormalization;

import com.dataspec.businessglossary.model.GlossaryMatch;
import com.dataspec.businessglossary.service.BusinessGlossaryService;
import com.dataspec.common.exception.BizException;
import com.dataspec.querynormalization.model.QueryNormalizationResult;
import com.dataspec.querynormalization.model.QueryTokenEvidence;
import com.dataspec.querynormalization.model.QueryTokenResolutionStatus;
import com.dataspec.querynormalization.service.impl.QueryNormalizationServiceImpl;
import com.dataspec.querynormalization.tokenizer.NameLexicalTokenizer;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.security.model.ApiTokenPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 项目级命名归一化测试。
 */
class QueryNormalizationServiceImplTest {

    @AfterEach
    void tearDown() {
        DataSpecSecurityContext.clear();
    }

    @Test
    void normalize_rejectsProjectOutsideTokenScopeBeforeGlossaryRead() {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        QueryNormalizationServiceImpl service = service(glossaryService);
        DataSpecSecurityContext.set(new ApiTokenPrincipal("limited", "tester", false, Set.of(2L)));

        BizException exception = org.junit.jupiter.api.Assertions.assertThrows(
                BizException.class,
                () -> service.normalize(1L, "amt"));

        assertEquals(403, exception.getCode());
        verifyNoInteractions(glossaryService);
    }

    @Test
    void normalize_exposesStableLexicalEvidenceWithoutGuessingAbbreviations() {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        when(glossaryService.match(1L, "ord_amt")).thenReturn(List.of());
        QueryNormalizationServiceImpl service = service(glossaryService);

        QueryNormalizationResult result = service.normalize(1L, "ord_amt");

        assertEquals("ord amt", result.normalizedText());
        assertEquals(List.of("ord", "amt"), result.queryTokens().stream()
                .map(QueryTokenEvidence::normalizedToken)
                .toList());
        assertEquals(List.of(QueryTokenResolutionStatus.UNRESOLVED, QueryTokenResolutionStatus.UNRESOLVED),
                result.queryTokens().stream().map(QueryTokenEvidence::resolutionStatus).toList());
        verify(glossaryService).match(1L, "ord_amt");
    }

    @Test
    void normalize_resolvesLongestChineseGlossaryMatch() {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        when(glossaryService.match(1L, "会员手机号")).thenReturn(List.of(match(
                3L, "会员手机号", "会员手机号", "TERM", 122, 30L, "member_mobile_no", false)));
        QueryNormalizationServiceImpl service = service(glossaryService);

        QueryTokenEvidence evidence = service.normalize(1L, "会员手机号").queryTokens().getFirst();

        assertEquals(QueryTokenResolutionStatus.RESOLVED, evidence.resolutionStatus());
        assertEquals("会员手机号", evidence.canonicalTerm());
        assertEquals(30L, evidence.canonicalFieldId());
        assertEquals(List.of(3L), evidence.glossaryIds());
    }

    @Test
    void normalize_foldsSameCanonicalAndRejectsConflictingAbbreviation() {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        when(glossaryService.match(1L, "amt")).thenReturn(List.of(new GlossaryMatch(
                4L,
                null,
                "amt",
                "ABBREVIATION",
                0,
                null,
                null,
                Set.of(),
                false,
                "同一缩写指向多个 canonical 字段",
                QueryTokenResolutionStatus.AMBIGUOUS,
                List.of(4L, 5L))));
        QueryNormalizationServiceImpl service = service(glossaryService);

        QueryTokenEvidence ambiguous = service.normalize(1L, "amt").queryTokens().getFirst();

        assertEquals(QueryTokenResolutionStatus.AMBIGUOUS, ambiguous.resolutionStatus());
        assertNull(ambiguous.canonicalTerm());
        assertNull(ambiguous.canonicalFieldId());
        assertEquals(List.of(4L, 5L), ambiguous.glossaryIds());
    }

    @Test
    void normalize_marksDisabledTermWithoutCanonicalConfidence() {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        when(glossaryService.match(1L, "老手机号")).thenReturn(List.of(match(
                6L, "手机号", "老手机号", "DISABLED", 0, 60L, "mobile_no", true)));
        QueryNormalizationServiceImpl service = service(glossaryService);

        QueryTokenEvidence disabled = service.normalize(1L, "老手机号").queryTokens().getFirst();

        assertEquals(QueryTokenResolutionStatus.DISABLED, disabled.resolutionStatus());
        assertNull(disabled.canonicalFieldId());
    }

    @Test
    void normalize_keepsDistinctChineseLongestMatchesResolved() {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        when(glossaryService.match(1L, "会员手机号订单金额")).thenReturn(List.of(
                match(7L, "会员手机号", "会员手机号", "TERM", 122, 30L, "mobile_no", false),
                match(8L, "订单金额", "订单金额", "TERM", 122, 40L, "order_amount", false)));
        QueryNormalizationServiceImpl service = service(glossaryService);

        List<QueryTokenEvidence> evidence = service.normalize(1L, "会员手机号订单金额").queryTokens();

        assertEquals(List.of("会员手机号", "订单金额"), evidence.stream()
                .map(QueryTokenEvidence::normalizedToken)
                .toList());
        assertEquals(List.of(QueryTokenResolutionStatus.RESOLVED, QueryTokenResolutionStatus.RESOLVED),
                evidence.stream().map(QueryTokenEvidence::resolutionStatus).toList());
        assertEquals(List.of(30L, 40L), evidence.stream().map(QueryTokenEvidence::canonicalFieldId).toList());
    }

    @Test
    void normalize_preservesRepeatedLexicalTokenEvidence() {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        when(glossaryService.match(1L, "id_id")).thenReturn(List.of());
        QueryNormalizationServiceImpl service = service(glossaryService);

        List<QueryTokenEvidence> evidence = service.normalize(1L, "id_id").queryTokens();

        assertEquals(List.of("id", "id"), evidence.stream()
                .map(QueryTokenEvidence::normalizedToken)
                .toList());
    }

    @Test
    void normalize_replacesMultiTokenGlossaryPhraseWithoutDuplicateUnresolvedTokens() {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        when(glossaryService.match(1L, "order amount")).thenReturn(List.of(match(
                9L, "order amount", "order amount", "TERM", 122, 40L, "order_amount", false)));
        QueryNormalizationServiceImpl service = service(glossaryService);

        List<QueryTokenEvidence> evidence = service.normalize(1L, "order amount").queryTokens();

        assertEquals(1, evidence.size());
        assertEquals("orderamount", evidence.getFirst().normalizedToken());
        assertEquals(QueryTokenResolutionStatus.RESOLVED, evidence.getFirst().resolutionStatus());
        assertEquals(40L, evidence.getFirst().canonicalFieldId());
    }

    @Test
    void normalize_placesRootSubstringEvidenceAtItsLexicalPosition() {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        when(glossaryService.match(1L, "payment_amount")).thenReturn(List.of(matchWithSpan(
                10L, "支付", "pay", "ROOT", 108, 40L, "payment_amount", 0, 3)));
        QueryNormalizationServiceImpl service = service(glossaryService);

        List<QueryTokenEvidence> evidence = service.normalize(1L, "payment_amount").queryTokens();

        assertEquals(List.of("pay", "ment", "amount"), evidence.stream()
                .map(QueryTokenEvidence::normalizedToken)
                .toList());
        assertEquals(List.of(
                        QueryTokenResolutionStatus.RESOLVED,
                        QueryTokenResolutionStatus.UNRESOLVED,
                        QueryTokenResolutionStatus.UNRESOLVED),
                evidence.stream().map(QueryTokenEvidence::resolutionStatus).toList());
    }

    @Test
    void normalize_keepsScoringRootEvidenceWhenEvidenceLimitIsReached() {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        String query = "payment_" + String.join("_", java.util.Collections.nCopies(31, "segment"));
        when(glossaryService.match(1L, query)).thenReturn(List.of(matchWithSpan(
                11L, "支付", "pay", "ROOT", 108, 40L, "payment_amount", 0, 3)));
        QueryNormalizationServiceImpl service = service(glossaryService);

        List<QueryTokenEvidence> evidence = service.normalize(1L, query).queryTokens();

        assertEquals(NameLexicalTokenizer.MAX_EVIDENCE_TOKEN_COUNT, evidence.size());
        assertTrue(evidence.stream().anyMatch(item ->
                "pay".equals(item.normalizedToken())
                        && item.resolutionStatus() == QueryTokenResolutionStatus.RESOLVED));
    }

    @Test
    void normalize_prioritizesAmbiguousAndDisabledEvidenceAtTheLimit() {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        List<String> tokens = java.util.stream.IntStream.range(0, 34)
                .mapToObj(this::alphabeticToken)
                .toList();
        List<GlossaryMatch> matches = new java.util.ArrayList<>();
        for (int index = 0; index < 32; index++) {
            matches.add(match(
                    (long) index + 1,
                    tokens.get(index),
                    tokens.get(index),
                    "TERM",
                    122,
                    40L,
                    "order_amount",
                    false));
        }
        matches.add(new GlossaryMatch(
                100L,
                null,
                tokens.get(32),
                "ABBREVIATION",
                0,
                null,
                null,
                Set.of(),
                false,
                "同一缩写指向多个 canonical 字段",
                QueryTokenResolutionStatus.AMBIGUOUS,
                List.of(100L, 101L)));
        matches.add(match(
                102L,
                "禁用词",
                tokens.get(33),
                "DISABLED",
                0,
                null,
                null,
                true));
        String query = String.join("_", tokens);
        when(glossaryService.match(1L, query)).thenReturn(matches);
        QueryNormalizationServiceImpl service = service(glossaryService);

        List<QueryTokenEvidence> evidence = service.normalize(1L, query).queryTokens();

        assertEquals(NameLexicalTokenizer.MAX_EVIDENCE_TOKEN_COUNT, evidence.size());
        assertTrue(evidence.stream().anyMatch(item ->
                item.resolutionStatus() == QueryTokenResolutionStatus.AMBIGUOUS));
        assertTrue(evidence.stream().anyMatch(item ->
                item.resolutionStatus() == QueryTokenResolutionStatus.DISABLED));
    }

    @Test
    void normalize_keepsBoundedMultiTokenSummaryBeyondSingleTokenLength() {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        String query = String.join("_", java.util.Collections.nCopies(20, "segment"));
        when(glossaryService.match(1L, query)).thenReturn(List.of());
        QueryNormalizationServiceImpl service = service(glossaryService);

        QueryNormalizationResult result = service.normalize(1L, query);

        assertEquals(String.join(" ", java.util.Collections.nCopies(20, "segment")), result.normalizedText());
    }

    @Test
    void normalize_keepsFullInternalTokenWhileBoundingPublicEvidenceText() {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        String query = "a".repeat(100);
        when(glossaryService.match(1L, query)).thenReturn(List.of());
        QueryNormalizationServiceImpl service = service(glossaryService);

        QueryNormalizationResult result = service.normalize(1L, query);

        assertEquals(query, result.lexicalTokens().getFirst().normalized());
        assertTrue(result.queryTokens().getFirst().normalizedToken().length()
                <= NameLexicalTokenizer.MAX_EVIDENCE_TOKEN_LENGTH);
    }

    @Test
    void normalize_keepsSupplementaryHanAtTheInputBoundaryWithoutUnpairedSurrogates() {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        when(glossaryService.match(anyLong(), anyString())).thenReturn(List.of());
        String supplementaryHan = new String(Character.toChars(0x20000));
        String query = "a".repeat(NameLexicalTokenizer.MAX_INPUT_LENGTH - 4) + supplementaryHan + "bbbb";
        QueryNormalizationServiceImpl service = service(glossaryService);

        QueryNormalizationResult result = service.normalize(1L, query);

        assertEquals(supplementaryHan, result.lexicalTokens().getLast().normalized());
        assertTrue(hasOnlyPairedSurrogates(result.normalizedText()));
        assertTrue(result.queryTokens().stream().allMatch(item ->
                hasOnlyPairedSurrogates(item.token()) && hasOnlyPairedSurrogates(item.normalizedToken())));
    }

    @Test
    void normalize_boundsEvidenceIdsReasonAndCanonicalText() {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        List<GlossaryMatch> matches = java.util.stream.LongStream.rangeClosed(1, 12)
                .mapToObj(id -> new GlossaryMatch(
                        id,
                        "术".repeat(100),
                        "amt",
                        "ABBREVIATION",
                        104,
                        30L,
                        "canonical_field_" + "x".repeat(100),
                        Set.of("order_amount"),
                        false,
                        "reason-" + "x".repeat(400),
                        QueryTokenResolutionStatus.RESOLVED,
                        List.of(id)))
                .toList();
        when(glossaryService.match(1L, "amt")).thenReturn(matches);
        QueryNormalizationServiceImpl service = service(glossaryService);

        QueryTokenEvidence evidence = service.normalize(1L, "amt").queryTokens().getFirst();

        assertEquals(8, evidence.glossaryIds().size());
        assertTrue(evidence.reason().length() <= 240);
        assertTrue(evidence.canonicalTerm().length() <= NameLexicalTokenizer.MAX_EVIDENCE_TOKEN_LENGTH);
        assertTrue(evidence.canonicalFieldName().length() <= NameLexicalTokenizer.MAX_EVIDENCE_TOKEN_LENGTH);
    }

    @Test
    void normalize_redactsSecretLikeQueryBeforeEvidenceAndGlossaryMatching() {
        BusinessGlossaryService glossaryService = mock(BusinessGlossaryService.class);
        when(glossaryService.match(1L, "token=[REDACTED]")).thenReturn(List.of());
        QueryNormalizationServiceImpl service = service(glossaryService);

        QueryNormalizationResult result = service.normalize(1L, "token=raw-secret-value");

        assertFalse(result.normalizedText().contains("raw-secret-value"));
        assertFalse(result.queryTokens().stream().anyMatch(item ->
                item.token().contains("raw-secret-value")
                        || item.normalizedToken().contains("raw-secret-value")
                        || item.reason().contains("raw-secret-value")));
        verify(glossaryService).match(1L, "token=[REDACTED]");
    }

    private QueryNormalizationServiceImpl service(BusinessGlossaryService glossaryService) {
        return new QueryNormalizationServiceImpl(new NameLexicalTokenizer(), glossaryService);
    }

    private GlossaryMatch match(Long id, String term, String token, String type, int score,
                                Long fieldId, String fieldName, boolean disabled) {
        return new GlossaryMatch(
                id,
                term,
                token,
                type,
                score,
                fieldId,
                fieldName,
                fieldName == null ? Set.of() : Set.of(fieldName),
                disabled,
                disabled ? "术语表禁用词：" + token : "术语表：" + token + " -> " + fieldName);
    }

    private GlossaryMatch matchWithSpan(
            Long id,
            String term,
            String token,
            String type,
            int score,
            Long fieldId,
            String fieldName,
            int startOffset,
            int endOffset
    ) {
        return new GlossaryMatch(
                id,
                term,
                token,
                type,
                score,
                fieldId,
                fieldName,
                Set.of(fieldName),
                false,
                "术语表：" + token + " -> " + fieldName,
                QueryTokenResolutionStatus.RESOLVED,
                List.of(id),
                startOffset,
                endOffset);
    }

    private String alphabeticToken(int index) {
        return new String(new char[]{(char) ('a' + index / 26), (char) ('a' + index % 26)});
    }

    private boolean hasOnlyPairedSurrogates(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (index + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    return false;
                }
                index += 1;
            } else if (Character.isLowSurrogate(current)) {
                return false;
            }
        }
        return true;
    }
}
