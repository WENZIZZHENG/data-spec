package com.dataspec.ruleexemption;

import com.dataspec.ruleexemption.controller.RuleExemptionController;
import com.dataspec.ruleexemption.entity.RuleExemption;
import com.dataspec.ruleexemption.service.RuleExemptionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class RuleExemptionControllerTest {

    @Test
    void list_returnsProjectExemptions() {
        RuleExemptionService service = mock(RuleExemptionService.class);
        RuleExemption exemption = new RuleExemption();
        exemption.setProjectId(1L);
        when(service.listByProject(1L)).thenReturn(List.of(exemption));
        RuleExemptionController controller = new RuleExemptionController(service);

        var response = controller.list(1L);

        assertEquals(1, response.getData().size());
        assertSame(exemption, response.getData().get(0));
    }

    @Test
    void create_delegatesToService() {
        RuleExemptionService service = mock(RuleExemptionService.class);
        RuleExemption saved = new RuleExemption();
        saved.setId(7L);
        when(service.create(any(RuleExemption.class))).thenReturn(saved);
        RuleExemptionController controller = new RuleExemptionController(service);

        var response = controller.create(new RuleExemptionController.RuleExemptionReq(
                1L,
                "table_naming_snake_case",
                "UserOrder",
                null,
                "历史表兼容",
                null
        ));

        assertSame(saved, response.getData());
        verify(service).create(argThat(exemption ->
                exemption.getProjectId().equals(1L)
                        && "table_naming_snake_case".equals(exemption.getRuleCode())
                        && "UserOrder".equals(exemption.getTableName())
                        && "历史表兼容".equals(exemption.getReason())));
    }

    @Test
    void disableAndDelete_delegateToService() {
        RuleExemptionService service = mock(RuleExemptionService.class);
        RuleExemptionController controller = new RuleExemptionController(service);

        controller.disable(8L);
        controller.delete(9L);

        verify(service).disable(8L);
        verify(service).delete(9L);
    }
}
