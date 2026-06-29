package com.dataspec.requirementdraft.model;

import java.util.List;

/**
 * 自然语言需求草案结果。
 */
public record RequirementDraftResult(
        Long projectId,
        String description,
        String targetTableName,
        String groupHint,
        List<RequirementMatchedField> matchedFields,
        List<RequirementMissingCandidate> missingCandidates,
        List<RequirementAmbiguousTerm> ambiguousTerms,
        RequirementRecommendedTemplate recommendedTemplate,
        List<String> nextActions,
        String copyablePrompt
) {
}
