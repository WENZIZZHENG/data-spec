package com.dataspec.querynormalization;

import com.dataspec.querynormalization.model.NameLexicalToken;
import com.dataspec.querynormalization.model.QueryTokenKind;
import com.dataspec.querynormalization.tokenizer.NameLexicalTokenizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 确定性命名词法拆分测试。
 */
class NameLexicalTokenizerTest {

    private final NameLexicalTokenizer tokenizer = new NameLexicalTokenizer();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void tokenize_matchesDeterministicGoldenFixture() throws Exception {
        JsonNode fixture = objectMapper.readTree(readResource(
                "fixtures/querynormalization/deterministic-name-tokenization.json"));

        assertEquals(1, fixture.path("schemaVersion").asInt());
        for (JsonNode scenario : fixture.path("lexicalCases")) {
            List<NameLexicalToken> tokens = tokenizer.tokenize(scenario.path("input").asText());
            String scenarioId = scenario.path("id").asText();

            assertEquals(stringValues(scenario.path("normalizedTokens")), normalized(tokens), scenarioId);
            assertEquals(
                    stringValues(scenario.path("tokenKinds")),
                    tokens.stream().map(token -> token.kind().name()).toList(),
                    scenarioId);
        }
    }

    @Test
    void tokenize_splitsAcronymCamelAndNumberBoundariesDeterministically() {
        List<NameLexicalToken> first = tokenizer.tokenize("HTTPStatus2Code");
        List<NameLexicalToken> second = tokenizer.tokenize("HTTPStatus2Code");

        assertEquals(List.of("http", "status", "2", "code"), normalized(first));
        assertEquals(List.of(
                QueryTokenKind.ACRONYM,
                QueryTokenKind.WORD,
                QueryTokenKind.NUMBER,
                QueryTokenKind.WORD), first.stream().map(NameLexicalToken::kind).toList());
        assertEquals(first, second);
    }

    @Test
    void tokenize_keepsSeparatorsHanAndBoundedUnitsDistinct() {
        List<NameLexicalToken> tokens = tokenizer.tokenize("ord_amt-100ms/会员ID");

        assertEquals(List.of("ord", "amt", "100", "ms", "会员", "id"), normalized(tokens));
        assertEquals(QueryTokenKind.NUMBER, tokens.get(2).kind());
        assertEquals(QueryTokenKind.UNIT, tokens.get(3).kind());
        assertEquals(QueryTokenKind.HAN, tokens.get(4).kind());
        assertEquals(QueryTokenKind.ACRONYM, tokens.get(5).kind());
    }

    @Test
    void tokenize_preservesDistinctLongInternalTokensAndBoundsOnlyTheInput() {
        String sharedPrefix = "a".repeat(64);
        String firstValue = sharedPrefix + "x";
        String secondValue = sharedPrefix + "y";

        NameLexicalToken first = tokenizer.tokenize(firstValue).getFirst();
        NameLexicalToken second = tokenizer.tokenize(secondValue).getFirst();

        assertEquals(firstValue, first.normalized());
        assertEquals(secondValue, second.normalized());
        assertNotEquals(first.normalized(), second.normalized());

        List<NameLexicalToken> manyTokens = tokenizer.tokenize(("a_").repeat(100));
        assertTrue(manyTokens.size() > NameLexicalTokenizer.MAX_EVIDENCE_TOKEN_COUNT);
        assertTrue(manyTokens.stream().mapToInt(token -> token.text().codePointCount(0, token.text().length())).sum()
                <= NameLexicalTokenizer.MAX_INPUT_LENGTH);
    }

    @Test
    void tokenize_keepsSupplementaryHanCodePoints() {
        String supplementaryHan = new String(Character.toChars(0x20000));

        List<NameLexicalToken> tokens = tokenizer.tokenize(supplementaryHan + "字段");

        assertEquals(List.of(supplementaryHan + "字段"), normalized(tokens));
        assertEquals(QueryTokenKind.HAN, tokens.getFirst().kind());
    }

    @Test
    void tokenize_classifiesSupplementaryNumbersAndSingleLettersByCodePoint() {
        String mathematicalZero = new String(Character.toChars(0x1D7D8));
        String deseretCapitalLetter = new String(Character.toChars(0x10400));

        assertEquals(QueryTokenKind.NUMBER, tokenizer.tokenize(mathematicalZero).getFirst().kind());
        assertEquals(QueryTokenKind.WORD, tokenizer.tokenize(deseretCapitalLetter).getFirst().kind());
    }

    private List<String> normalized(List<NameLexicalToken> tokens) {
        return tokens.stream().map(NameLexicalToken::normalized).toList();
    }

    private List<String> stringValues(JsonNode values) {
        return java.util.stream.StreamSupport.stream(values.spliterator(), false)
                .map(JsonNode::asText)
                .toList();
    }

    private String readResource(String path) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "测试资源不存在: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
