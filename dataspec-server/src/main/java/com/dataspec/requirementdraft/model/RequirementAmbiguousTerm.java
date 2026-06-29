package com.dataspec.requirementdraft.model;

import java.util.List;

/**
 * 需求草案中不能安全自动选择的词项。
 */
public record RequirementAmbiguousTerm(
        String term,
        String reason,
        List<RequirementAmbiguousCandidate> candidates
) {
}
