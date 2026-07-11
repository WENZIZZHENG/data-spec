package com.dataspec.generator;

import com.dataspec.generator.controller.GeneratorController;
import com.dataspec.generator.model.DdlGenerateResult;
import com.dataspec.generator.model.DdlStructureSummary;
import com.dataspec.generator.service.DdlGeneratorService;
import com.dataspec.generator.service.HtmlDataDictionaryService;
import com.dataspec.generator.service.MarkdownGeneratorService;
import com.dataspec.lint.model.LintResult;
import com.dataspec.standard.dto.StandardSnapshotInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 生成器接口测试
 */
class GeneratorControllerTest {

    @Test
    void previewDdl_returnsGeneratedDdlResult() {
        MarkdownGeneratorService markdownGeneratorService = mock(MarkdownGeneratorService.class);
        HtmlDataDictionaryService htmlDataDictionaryService = mock(HtmlDataDictionaryService.class);
        DdlGeneratorService ddlGeneratorService = mock(DdlGeneratorService.class);
        GeneratorController controller = new GeneratorController(
                markdownGeneratorService,
                htmlDataDictionaryService,
                ddlGeneratorService);
        DdlGenerateResult result = new DdlGenerateResult(
                "CREATE TABLE user_order (id bigserial);",
                LintResult.of(List.of(), List.of()),
                StandardSnapshotInfo.unversioned(1L),
                List.of(),
                DdlStructureSummary.empty()
        );
        when(ddlGeneratorService.generateFromTemplate(1L, 10L, "user_order")).thenReturn(result);

        var response = controller.previewDdl(1L, 10L, "user_order");

        assertEquals(200, response.getCode());
        assertEquals("CREATE TABLE user_order (id bigserial);", response.getData().ddl());
    }
}
