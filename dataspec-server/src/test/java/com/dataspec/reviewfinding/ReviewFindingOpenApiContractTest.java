package com.dataspec.reviewfinding;

import com.dataspec.aioutputcheck.model.AiOutputPostCheckRequest;
import com.dataspec.aioutputcheck.model.AiOutputPostCheckResult;
import com.dataspec.evidence.model.AiEvidencePackage;
import com.dataspec.evidence.model.AiEvidencePackageReq;
import com.dataspec.lint.model.LintResult;
import com.dataspec.reviewfinding.model.ReviewFinding;
import com.dataspec.reviewfinding.model.ReviewFindingLocation;
import com.dataspec.reviewfinding.model.ReviewFindingSubject;
import com.dataspec.reviewfinding.model.ReviewFindingWaiver;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReviewFindingOpenApiContractTest {

    @Test
    void sharedFindingAndNestedModelsDocumentEveryPublicField() {
        for (Class<?> type : List.of(
                ReviewFinding.class,
                ReviewFindingSubject.class,
                ReviewFindingLocation.class,
                ReviewFindingWaiver.class)) {
            Schema typeSchema = type.getAnnotation(Schema.class);
            assertNotNull(typeSchema, type.getSimpleName() + " 缺少 @Schema");
            assertFalse(typeSchema.description().isBlank(), type.getSimpleName() + " 缺少 schema description");
            for (Field field : type.getDeclaredFields()) {
                if (field.isSynthetic() || java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                assertDocumented(field);
            }
        }
    }

    @Test
    void additiveFindingFieldsHaveOpenApiDescriptions() throws Exception {
        assertDocumented(LintResult.class.getDeclaredField("findings"));
        assertDocumented(LintResult.class.getDeclaredField("sqlCheckRecordId"));
        assertDocumented(AiOutputPostCheckRequest.class.getDeclaredField("findings"));
        assertDocumented(AiOutputPostCheckResult.class.getDeclaredField("findings"));
        assertDocumented(AiOutputPostCheckResult.class.getDeclaredField("verificationReceipt"));
        assertDocumented(AiEvidencePackageReq.class.getDeclaredField("findings"));
        assertDocumented(AiEvidencePackageReq.class.getDeclaredField("postCheckReceipt"));
        assertDocumented(AiEvidencePackage.class.getDeclaredField("findings"));
    }

    @Test
    void findingTextFieldsExposeMachineReadableBounds() throws Exception {
        assertTextBound(ReviewFinding.class, "findingKey", 128);
        assertTextBound(ReviewFinding.class, "code", 128);
        assertTextBound(ReviewFinding.class, "trigger", 1000);
        assertTextBound(ReviewFinding.class, "expected", 1000);
        assertTextBound(ReviewFinding.class, "observed", 1000);
        assertTextBound(ReviewFinding.class, "suggestedFix", 1000);
        assertTextBound(ReviewFindingSubject.class, "kind", 64);
        assertTextBound(ReviewFindingSubject.class, "name", 256);
        assertTextBound(ReviewFindingSubject.class, "tableName", 256);
        assertTextBound(ReviewFindingSubject.class, "columnName", 256);
        assertTextBound(ReviewFindingSubject.class, "stableRef", 256);
        assertTextBound(ReviewFindingLocation.class, "path", 512);
        assertTextBound(ReviewFindingLocation.class, "locationKind", 64);
        assertTextBound(ReviewFindingWaiver.class, "reason", 500);
        Field code = ReviewFinding.class.getDeclaredField("code");
        assertNotNull(code.getAnnotation(NotBlank.class));
        assertEquals(1, code.getAnnotation(Schema.class).minLength());
    }

    private void assertDocumented(Field field) {
        Schema schema = field.getAnnotation(Schema.class);
        ArraySchema arraySchema = field.getAnnotation(ArraySchema.class);
        assertFalse(schema == null && arraySchema == null, field + " 缺少 OpenAPI 字段说明");
        String description = schema != null
                ? schema.description()
                : arraySchema.arraySchema().description();
        assertFalse(description.isBlank(), field + " 的 OpenAPI description 为空");
    }

    private void assertTextBound(Class<?> type, String fieldName, int expectedMaxLength) throws Exception {
        Schema schema = type.getDeclaredField(fieldName).getAnnotation(Schema.class);
        assertNotNull(schema, type.getSimpleName() + "." + fieldName + " 缺少 @Schema");
        assertEquals(expectedMaxLength, schema.maxLength(), type.getSimpleName() + "." + fieldName + " maxLength 漂移");
    }
}
