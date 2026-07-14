package com.dataspec.reviewfinding;

import com.dataspec.aioutputcheck.model.AiOutputPostCheckRequest;
import com.dataspec.evidence.model.AiEvidencePackageReq;
import com.dataspec.reviewfinding.model.ReviewFinding;
import com.dataspec.reviewfinding.model.ReviewFindingSeverity;
import com.dataspec.reviewfinding.model.ReviewFindingSource;
import com.dataspec.reviewfinding.model.ReviewFindingWaiver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewFindingValidationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void nullFindingElementsSurviveDeserializationAndFailBeanValidation() throws Exception {
        AiOutputPostCheckRequest postCheck = objectMapper.readValue("""
                {"projectId":1,"contentType":"TEXT","content":"review","findings":[null]}
                """, AiOutputPostCheckRequest.class);
        AiEvidencePackageReq evidencePackage = objectMapper.readValue("""
                {"projectId":1,"sourceType":"SQL_CHECK","sourceId":7,"findings":[null]}
                """, AiEvidencePackageReq.class);

        assertEquals(1, postCheck.findings().size());
        assertNull(postCheck.findings().getFirst());
        assertEquals(1, evidencePackage.findings().size());
        assertNull(evidencePackage.findings().getFirst());
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            assertTrue(validator.validate(postCheck).stream()
                    .anyMatch(violation -> violation.getMessage().contains("findings 元素不能为空")));
            assertTrue(validator.validate(evidencePackage).stream()
                    .anyMatch(violation -> violation.getMessage().contains("findings 元素不能为空")));
        }
    }

    @Test
    void blankFindingCodeFailsBeanValidation() {
        ReviewFinding finding = new ReviewFinding(
                ReviewFindingSource.EXTERNAL_AI,
                null,
                " ",
                ReviewFindingSeverity.WARNING,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                false,
                ReviewFindingWaiver.NONE);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(finding).stream()
                    .anyMatch(violation -> violation.getMessage().contains("code 不能为空")));
        }
    }
}
