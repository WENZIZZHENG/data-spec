package com.dataspec.aiprofile;

import com.dataspec.aiprofile.model.AiProfileDiagnostic;
import com.dataspec.aiprofile.model.AiTaskProfile;
import com.dataspec.aiprofile.model.AiTaskProfileCatalog;
import com.dataspec.aiprofile.model.AiTaskProfileDetail;
import com.dataspec.aiprofile.service.impl.AiTaskProfileServiceImpl;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.lint.model.FixMode;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.service.RuleConfigService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiTaskProfileServiceImplTest {

    @Test
    void listProfilesReturnsBuiltInsAndProjectDiagnostics() {
        FieldService fieldService = mock(FieldService.class);
        RuleConfigService ruleConfigService = mock(RuleConfigService.class);
        when(fieldService.listByProject(1L)).thenReturn(List.of(field("user_id")));
        when(ruleConfigService.listEnabledByProject(1L)).thenReturn(List.of(rule("table_naming_snake_case")));
        AiTaskProfileServiceImpl service = new AiTaskProfileServiceImpl(fieldService, ruleConfigService);

        AiTaskProfileCatalog catalog = service.listProfiles(1L, "sql-fix");

        assertEquals(1L, catalog.getProjectId());
        assertEquals("create-table", catalog.getDefaultProfileId());
        assertEquals("sql-fix", catalog.getSelectedProfileId());
        assertEquals(5, catalog.getProfiles().size());
        AiTaskProfile selected = catalog.getProfiles().stream()
                .filter(AiTaskProfile::getDefaultProfile)
                .findFirst()
                .orElseThrow();
        assertEquals("sql-fix", selected.getProfileId());
        assertEquals("SQL_FIX", selected.getTaskType());
        assertEquals(FixMode.DRY_RUN, selected.getFixedSqlPolicy().getMode());
        assertTrue(catalog.getSupportedTaskTypes().contains("REVERSE_IMPORT"));
        assertTrue(catalog.getDiagnostics().stream()
                .anyMatch(item -> "PROFILE_READY".equals(item.getCode()) && "pass".equals(item.getStatus())));
    }

    @Test
    void getProfileSupportsTaskTypeLookup() {
        AiTaskProfileServiceImpl service = new AiTaskProfileServiceImpl(mock(FieldService.class), mock(RuleConfigService.class));

        AiTaskProfileDetail detail = service.getProfile(null, "SQL_FIX");

        assertEquals("SQL_FIX", detail.getRequestedProfile());
        assertEquals("sql-fix", detail.getProfile().getProfileId());
        assertEquals(FixMode.DRY_RUN, detail.getProfile().getFixedSqlPolicy().getMode());
        assertTrue(detail.getDiagnostics().stream()
                .anyMatch(item -> "MISSING_PROJECT".equals(item.getCode()) && "warn".equals(item.getStatus())));
    }

    @Test
    void unknownProfileReturnsStableDiagnosticAndSupportedValues() {
        AiTaskProfileServiceImpl service = new AiTaskProfileServiceImpl(mock(FieldService.class), mock(RuleConfigService.class));

        AiTaskProfileDetail detail = service.getProfile(1L, "does-not-exist");

        assertNull(detail.getProfile());
        assertTrue(detail.getSupportedProfileIds().contains("create-table"));
        assertTrue(detail.getSupportedTaskTypes().contains("SQL_FIX"));
        AiProfileDiagnostic diagnostic = detail.getDiagnostics().get(0);
        assertEquals("UNKNOWN_AI_PROFILE", diagnostic.getCode());
        assertEquals("fail", diagnostic.getStatus());
        assertTrue(diagnostic.getNextAction().contains("create-table"));
    }

    @Test
    void listProfilesFallsBackToDefaultWhenSelectedProfileIsUnknown() {
        AiTaskProfileServiceImpl service = new AiTaskProfileServiceImpl(mock(FieldService.class), mock(RuleConfigService.class));

        AiTaskProfileCatalog catalog = service.listProfiles(null, "missing-profile");

        assertEquals("create-table", catalog.getSelectedProfileId());
        assertFalse(catalog.getDiagnostics().isEmpty());
        assertTrue(catalog.getDiagnostics().stream()
                .anyMatch(item -> "UNKNOWN_AI_PROFILE".equals(item.getCode())));
    }

    private static Field field(String name) {
        Field field = new Field();
        field.setName(name);
        return field;
    }

    private static RuleConfig rule(String code) {
        RuleConfig rule = new RuleConfig();
        rule.setRuleCode(code);
        rule.setEnabled(true);
        return rule;
    }
}
