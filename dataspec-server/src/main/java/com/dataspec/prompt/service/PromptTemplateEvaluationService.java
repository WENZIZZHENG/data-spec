package com.dataspec.prompt.service;

import com.dataspec.prompt.model.PromptTemplateDefinition;
import com.dataspec.prompt.model.PromptTemplateEvalFailure;
import com.dataspec.prompt.model.PromptTemplateEvalResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 本地 Prompt 模板评测服务。
 *
 * <p>第一版只做确定性契约检查和 golden diff，不调用外部 LLM，保证 `mvn test` 可稳定复现。</p>
 */
@Service
@RequiredArgsConstructor
public class PromptTemplateEvaluationService {

    private final PromptTemplateRegistry registry;

    public PromptTemplateEvalResult evaluate(String templateKey, String output) {
        PromptTemplateDefinition template = registry.getTemplate(templateKey);
        List<PromptTemplateEvalFailure> failures = evaluateMarkers(template, output);
        return new PromptTemplateEvalResult(
                template.templateKey(),
                template.promptVersion(),
                failures.isEmpty(),
                failures,
                template.requiredSections(),
                template.requiredPhrases(),
                null
        );
    }

    public PromptTemplateEvalResult evaluateAgainstGolden(String templateKey, String output, String golden) {
        PromptTemplateDefinition template = registry.getTemplate(templateKey);
        List<PromptTemplateEvalFailure> failures = evaluateMarkers(template, output);
        String diff = diff(golden, output);
        if (diff != null) {
            failures = new ArrayList<>(failures);
            failures.add(new PromptTemplateEvalFailure(
                    "GOLDEN_DIFF",
                    template.promptVersion(),
                    "Prompt 输出与 golden fixture 不一致"
            ));
        }
        return new PromptTemplateEvalResult(
                template.templateKey(),
                template.promptVersion(),
                failures.isEmpty(),
                failures,
                template.requiredSections(),
                template.requiredPhrases(),
                diff
        );
    }

    public String diff(String expected, String actual) {
        String normalizedExpected = normalize(expected);
        String normalizedActual = normalize(actual);
        if (Objects.equals(normalizedExpected, normalizedActual)) {
            return null;
        }
        String[] expectedLines = normalizedExpected.split("\n", -1);
        String[] actualLines = normalizedActual.split("\n", -1);
        int max = Math.max(expectedLines.length, actualLines.length);
        StringBuilder diff = new StringBuilder();
        for (int i = 0; i < max; i++) {
            String expectedLine = i < expectedLines.length ? expectedLines[i] : null;
            String actualLine = i < actualLines.length ? actualLines[i] : null;
            if (!Objects.equals(expectedLine, actualLine)) {
                diff.append("@@ line ").append(i + 1).append(" @@\n");
                if (expectedLine != null) {
                    diff.append("-").append(expectedLine).append("\n");
                }
                if (actualLine != null) {
                    diff.append("+").append(actualLine).append("\n");
                }
            }
        }
        return diff.toString();
    }

    private List<PromptTemplateEvalFailure> evaluateMarkers(PromptTemplateDefinition template, String output) {
        List<PromptTemplateEvalFailure> failures = new ArrayList<>();
        String content = output == null ? "" : output;
        for (String section : template.requiredSections()) {
            if (!content.contains(section)) {
                failures.add(new PromptTemplateEvalFailure(
                        "MISSING_SECTION",
                        section,
                        "Prompt 缺少必备段落: " + section
                ));
            }
        }
        for (String phrase : template.requiredPhrases()) {
            if (!content.contains(phrase)) {
                failures.add(new PromptTemplateEvalFailure(
                        "MISSING_PHRASE",
                        phrase,
                        "Prompt 缺少必备短语: " + phrase
                ));
            }
        }
        return failures;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r\n", "\n").replace('\r', '\n').stripTrailing();
    }
}
