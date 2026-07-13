package com.dataspec.querynormalization.service.impl;

import com.dataspec.businessglossary.model.GlossaryMatch;
import com.dataspec.businessglossary.service.BusinessGlossaryService;
import com.dataspec.common.exception.BizException;
import com.dataspec.common.sanitize.SensitiveDataSanitizer;
import com.dataspec.querynormalization.model.NameLexicalToken;
import com.dataspec.querynormalization.model.QueryNormalizationResult;
import com.dataspec.querynormalization.model.QueryTokenEvidence;
import com.dataspec.querynormalization.model.QueryTokenKind;
import com.dataspec.querynormalization.model.QueryTokenResolution;
import com.dataspec.querynormalization.model.QueryTokenResolutionStatus;
import com.dataspec.querynormalization.service.QueryNormalizationService;
import com.dataspec.querynormalization.tokenizer.NameLexicalTokenizer;
import com.dataspec.security.context.ProjectAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 基于共享词法 tokenizer 和当前项目 glossary 的确定性查询归一化实现。
 */
@Service
@RequiredArgsConstructor
public class QueryNormalizationServiceImpl implements QueryNormalizationService {

    private static final int MAX_GLOSSARY_IDS = 8;
    private static final int MAX_REASON_LENGTH = 240;

    private final NameLexicalTokenizer tokenizer;
    private final BusinessGlossaryService businessGlossaryService;

    @Override
    public QueryNormalizationResult normalize(Long projectId, String query) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        ProjectAccessGuard.requireProjectAccess(projectId);
        String safeQuery = SensitiveDataSanitizer.redactText(query, NameLexicalTokenizer.MAX_INPUT_LENGTH);
        List<NameLexicalToken> lexicalTokens = tokenizer.tokenize(safeQuery);
        if (lexicalTokens.isEmpty()) {
            return new QueryNormalizationResult("", List.of(), List.of(), List.of(), List.of());
        }
        List<GlossaryMatch> matches = businessGlossaryService.match(projectId, safeQuery);
        List<GlossaryMatch> safeMatches = matches == null ? List.of() : List.copyOf(matches);
        Map<String, List<GlossaryMatch>> matchGroups = groupMatches(safeMatches);
        List<QueryTokenResolution> resolutions = new ArrayList<>();
        Set<String> representedMatches = new LinkedHashSet<>();
        int tokenIndex = 0;
        while (tokenIndex < lexicalTokens.size()) {
            RootSubstringEvidence rootEvidence = rootSubstringEvidenceAt(
                    lexicalTokens,
                    tokenIndex,
                    safeMatches);
            if (rootEvidence != null) {
                resolutions.addAll(rootEvidence.resolutions());
                representedMatches.addAll(rootEvidence.representedMatches());
                tokenIndex += 1;
                continue;
            }
            MatchGroup sequenceMatch = sequenceMatchAt(lexicalTokens, tokenIndex, matchGroups);
            if (sequenceMatch != null) {
                resolutions.add(resolutionFor(
                        sequenceMatch.displayToken(),
                        sequenceMatch.normalizedToken(),
                        sequenceMatch.kind(),
                        sequenceMatch.matches()));
                representedMatches.add(sequenceMatch.normalizedToken());
                tokenIndex += sequenceMatch.tokenCount();
                continue;
            }
            NameLexicalToken token = lexicalTokens.get(tokenIndex);
            if (token.kind() == QueryTokenKind.HAN) {
                appendHanEvidence(token, matchGroups, resolutions, representedMatches);
            } else {
                resolutions.add(resolutionFor(token.text(), token.normalized(), token.kind(), List.of()));
            }
            tokenIndex += 1;
        }
        for (Map.Entry<String, List<GlossaryMatch>> entry : matchGroups.entrySet()) {
            if (representedMatches.add(entry.getKey())) {
                GlossaryMatch first = entry.getValue().getFirst();
                resolutions.add(resolutionFor(
                        first.matchedToken(),
                        entry.getKey(),
                        matchKind(first.matchedToken()),
                        entry.getValue()));
            }
        }
        String normalizedText = String.join(" ", lexicalTokens.stream().map(NameLexicalToken::normalized).toList());
        List<QueryTokenResolution> boundedResolutions = boundResolutions(resolutions);
        return new QueryNormalizationResult(
                SensitiveDataSanitizer.redactText(normalizedText, NameLexicalTokenizer.MAX_INPUT_LENGTH),
                List.copyOf(lexicalTokens),
                boundedResolutions.stream().map(QueryTokenResolution::evidence).toList(),
                safeMatches,
                boundedResolutions);
    }

    private RootSubstringEvidence rootSubstringEvidenceAt(
            List<NameLexicalToken> lexicalTokens,
            int tokenIndex,
            List<GlossaryMatch> matches
    ) {
        NameLexicalToken lexicalToken = lexicalTokens.get(tokenIndex);
        int tokenStart = compactOffset(lexicalTokens, tokenIndex);
        int tokenEnd = tokenStart + lexicalToken.normalized().length();
        List<GlossaryMatch> rootMatches = matches.stream()
                .filter(match -> "ROOT".equals(match.matchType()))
                .filter(match -> match.startOffset() >= tokenStart && match.endOffset() <= tokenEnd)
                .filter(match -> match.startOffset() > tokenStart || match.endOffset() < tokenEnd)
                .sorted(Comparator
                        .comparingInt(GlossaryMatch::startOffset)
                        .thenComparingInt(GlossaryMatch::endOffset))
                .toList();
        if (rootMatches.isEmpty()) {
            return null;
        }
        List<QueryTokenResolution> result = new ArrayList<>();
        Set<String> represented = new LinkedHashSet<>();
        int cursor = tokenStart;
        for (GlossaryMatch match : rootMatches) {
            if (match.startOffset() < cursor || match.endOffset() <= match.startOffset()) {
                continue;
            }
            appendUnresolvedSegment(result, lexicalToken, tokenStart, cursor, match.startOffset());
            String normalizedMatch = normalizeMatchToken(match.matchedToken());
            result.add(resolutionFor(
                    match.matchedToken(),
                    normalizedMatch,
                    lexicalToken.kind(),
                    List.of(match)));
            represented.add(normalizedMatch);
            cursor = match.endOffset();
        }
        appendUnresolvedSegment(result, lexicalToken, tokenStart, cursor, tokenEnd);
        return result.isEmpty()
                ? null
                : new RootSubstringEvidence(List.copyOf(result), Set.copyOf(represented));
    }

    private void appendUnresolvedSegment(
            List<QueryTokenResolution> resolutions,
            NameLexicalToken lexicalToken,
            int tokenStart,
            int segmentStart,
            int segmentEnd
    ) {
        if (segmentEnd <= segmentStart) {
            return;
        }
        String segment = lexicalToken.normalized().substring(
                segmentStart - tokenStart,
                segmentEnd - tokenStart);
        resolutions.add(resolutionFor(segment, segment, lexicalToken.kind(), List.of()));
    }

    private int compactOffset(List<NameLexicalToken> tokens, int tokenIndex) {
        int offset = 0;
        for (int index = 0; index < tokenIndex; index++) {
            offset += tokens.get(index).normalized().length();
        }
        return offset;
    }

    private List<QueryTokenResolution> boundResolutions(List<QueryTokenResolution> resolutions) {
        int limit = NameLexicalTokenizer.MAX_EVIDENCE_TOKEN_COUNT;
        if (resolutions.size() <= limit) {
            return List.copyOf(resolutions);
        }
        boolean[] retained = new boolean[resolutions.size()];
        int retainedCount = 0;
        // 人工确认信号不能被前置 resolved token 挤出；最终输出仍保持原始 token 顺序。
        for (int index = 0; index < resolutions.size() && retainedCount < limit; index++) {
            QueryTokenResolutionStatus status = resolutions.get(index).evidence().resolutionStatus();
            if (status == QueryTokenResolutionStatus.AMBIGUOUS
                    || status == QueryTokenResolutionStatus.DISABLED) {
                retained[index] = true;
                retainedCount += 1;
            }
        }
        for (int index = 0; index < resolutions.size() && retainedCount < limit; index++) {
            if (!retained[index]
                    && resolutions.get(index).evidence().resolutionStatus() == QueryTokenResolutionStatus.RESOLVED) {
                retained[index] = true;
                retainedCount += 1;
            }
        }
        for (int index = 0; index < resolutions.size() && retainedCount < limit; index++) {
            if (!retained[index]) {
                retained[index] = true;
                retainedCount += 1;
            }
        }
        List<QueryTokenResolution> bounded = new ArrayList<>(limit);
        for (int index = 0; index < resolutions.size(); index++) {
            if (retained[index]) {
                bounded.add(resolutions.get(index));
            }
        }
        return List.copyOf(bounded);
    }

    private Map<String, List<GlossaryMatch>> groupMatches(List<GlossaryMatch> matches) {
        Map<String, List<GlossaryMatch>> groups = new LinkedHashMap<>();
        for (GlossaryMatch match : matches) {
            String normalized = normalizeMatchToken(match.matchedToken());
            if (!normalized.isBlank()) {
                groups.computeIfAbsent(normalized, ignored -> new ArrayList<>()).add(match);
            }
        }
        return groups;
    }

    private MatchGroup sequenceMatchAt(
            List<NameLexicalToken> lexicalTokens,
            int tokenIndex,
            Map<String, List<GlossaryMatch>> matchGroups
    ) {
        MatchGroup best = null;
        for (Map.Entry<String, List<GlossaryMatch>> entry : matchGroups.entrySet()) {
            GlossaryMatch first = entry.getValue().getFirst();
            List<NameLexicalToken> matchTokens = tokenizer.tokenize(first.matchedToken());
            if (matchTokens.isEmpty() || tokenIndex + matchTokens.size() > lexicalTokens.size()) {
                continue;
            }
            boolean matches = true;
            for (int offset = 0; offset < matchTokens.size(); offset++) {
                if (!Objects.equals(
                        lexicalTokens.get(tokenIndex + offset).normalized(),
                        matchTokens.get(offset).normalized())) {
                    matches = false;
                    break;
                }
            }
            if (!matches) {
                continue;
            }
            QueryTokenKind kind = matchTokens.size() == 1
                    ? lexicalTokens.get(tokenIndex).kind()
                    : matchKind(first.matchedToken());
            MatchGroup candidate = new MatchGroup(
                    entry.getKey(),
                    first.matchedToken(),
                    kind,
                    matchTokens.size(),
                    List.copyOf(entry.getValue()));
            if (best == null
                    || candidate.tokenCount() > best.tokenCount()
                    || (candidate.tokenCount() == best.tokenCount()
                    && candidate.normalizedToken().length() > best.normalizedToken().length())) {
                best = candidate;
            }
        }
        return best;
    }

    private void appendHanEvidence(
            NameLexicalToken token,
            Map<String, List<GlossaryMatch>> matchGroups,
            List<QueryTokenResolution> resolutions,
            Set<String> representedMatches
    ) {
        int cursor = 0;
        while (cursor < token.normalized().length()) {
            Map.Entry<String, List<GlossaryMatch>> next = null;
            int nextStart = Integer.MAX_VALUE;
            for (Map.Entry<String, List<GlossaryMatch>> entry : matchGroups.entrySet()) {
                if (!containsHan(entry.getKey())) {
                    continue;
                }
                int start = token.normalized().indexOf(entry.getKey(), cursor);
                if (start >= 0 && (start < nextStart
                        || (start == nextStart && (next == null || entry.getKey().length() > next.getKey().length())))) {
                    next = entry;
                    nextStart = start;
                }
            }
            if (next == null) {
                String unresolved = token.normalized().substring(cursor);
                resolutions.add(resolutionFor(unresolved, unresolved, QueryTokenKind.HAN, List.of()));
                return;
            }
            if (nextStart > cursor) {
                String unresolved = token.normalized().substring(cursor, nextStart);
                resolutions.add(resolutionFor(unresolved, unresolved, QueryTokenKind.HAN, List.of()));
            }
            GlossaryMatch first = next.getValue().getFirst();
            resolutions.add(resolutionFor(
                    first.matchedToken(),
                    next.getKey(),
                    QueryTokenKind.HAN,
                    next.getValue()));
            representedMatches.add(next.getKey());
            cursor = nextStart + next.getKey().length();
        }
    }

    private QueryTokenKind matchKind(String matchedToken) {
        List<NameLexicalToken> tokens = tokenizer.tokenize(matchedToken);
        if (tokens.stream().anyMatch(token -> token.kind() == QueryTokenKind.HAN)) {
            return QueryTokenKind.HAN;
        }
        return tokens.size() == 1 ? tokens.getFirst().kind() : QueryTokenKind.WORD;
    }

    private QueryTokenResolution resolutionFor(
            String token,
            String normalizedToken,
            QueryTokenKind kind,
            List<GlossaryMatch> matches
    ) {
        List<GlossaryMatch> exactMatches = matches == null ? List.of() : List.copyOf(matches);
        return new QueryTokenResolution(
                evidenceFor(token, normalizedToken, kind, exactMatches),
                exactMatches);
    }

    private QueryTokenEvidence evidenceFor(
            String token,
            String normalizedToken,
            QueryTokenKind kind,
            List<GlossaryMatch> matches
    ) {
        if (matches == null || matches.isEmpty()) {
            return new QueryTokenEvidence(
                    safeText(token),
                    safeText(normalizedToken),
                    kind,
                    QueryTokenResolutionStatus.UNRESOLVED,
                    null,
                    null,
                    null,
                    List.of(),
                    "当前项目 glossary 未提供唯一解析，不做猜测");
        }
        List<Long> glossaryIds = matches.stream()
                .flatMap(match -> {
                    if (match.glossaryIds() != null && !match.glossaryIds().isEmpty()) {
                        return match.glossaryIds().stream();
                    }
                    return match.glossaryId() == null ? java.util.stream.Stream.empty() : java.util.stream.Stream.of(match.glossaryId());
                })
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .limit(MAX_GLOSSARY_IDS)
                .toList();
        if (matches.stream().anyMatch(match ->
                match.disabledTerm() || match.resolutionStatus() == QueryTokenResolutionStatus.DISABLED)) {
            return new QueryTokenEvidence(
                    safeText(token),
                    safeText(normalizedToken),
                    kind,
                    QueryTokenResolutionStatus.DISABLED,
                    null,
                    null,
                    null,
                    glossaryIds,
                    safeReason("命中当前项目 glossary 禁用词，需要人工确认"));
        }
        if (matches.stream().anyMatch(match -> match.resolutionStatus() == QueryTokenResolutionStatus.AMBIGUOUS)) {
            return new QueryTokenEvidence(
                    safeText(token),
                    safeText(normalizedToken),
                    kind,
                    QueryTokenResolutionStatus.AMBIGUOUS,
                    null,
                    null,
                    null,
                    glossaryIds,
                    safeReason("同一 token 指向多个 canonical 字段，需要人工确认"));
        }
        Set<Long> canonicalFieldIds = matches.stream()
                .map(GlossaryMatch::canonicalFieldId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (canonicalFieldIds.size() > 1) {
            return new QueryTokenEvidence(
                    safeText(token),
                    safeText(normalizedToken),
                    kind,
                    QueryTokenResolutionStatus.AMBIGUOUS,
                    null,
                    null,
                    null,
                    glossaryIds,
                    safeReason("同一 token 指向多个 canonical 字段，需要人工确认"));
        }
        GlossaryMatch best = matches.stream()
                .max(Comparator.comparingInt(GlossaryMatch::score))
                .orElseThrow();
        return new QueryTokenEvidence(
                safeText(token),
                safeText(normalizedToken),
                kind,
                QueryTokenResolutionStatus.RESOLVED,
                safeText(best.term()),
                best.canonicalFieldId(),
                safeText(best.canonicalFieldName()),
                glossaryIds,
                safeReason(best.reason()));
    }

    private String normalizeMatchToken(String value) {
        return tokenizer.tokenize(value).stream()
                .map(NameLexicalToken::normalized)
                .collect(java.util.stream.Collectors.joining(""));
    }

    private String safeReason(String reason) {
        return SensitiveDataSanitizer.redactText(reason, MAX_REASON_LENGTH - 3);
    }

    private String safeText(String value) {
        return SensitiveDataSanitizer.redactText(value, NameLexicalTokenizer.MAX_EVIDENCE_TOKEN_LENGTH - 3);
    }

    private boolean containsHan(String value) {
        return value != null && value.codePoints()
                .anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    private record MatchGroup(
            String normalizedToken,
            String displayToken,
            QueryTokenKind kind,
            int tokenCount,
            List<GlossaryMatch> matches
    ) {
    }

    private record RootSubstringEvidence(
            List<QueryTokenResolution> resolutions,
            Set<String> representedMatches
    ) {
    }
}
