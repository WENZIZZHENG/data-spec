package com.dataspec.requirementdraft;

import com.dataspec.requirementdraft.controller.RequirementDraftController;
import com.dataspec.requirementdraft.model.RequirementDraftReq;
import com.dataspec.requirementdraft.model.RequirementDraftResult;
import com.dataspec.requirementdraft.service.RequirementDraftService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequirementDraftControllerTest {

    @Test
    void draft_delegatesToService() {
        RequirementDraftService service = mock(RequirementDraftService.class);
        RequirementDraftReq req = new RequirementDraftReq(1L, "会员支付流水表", "pay_trade", "payment", 10);
        RequirementDraftResult result = new RequirementDraftResult(
                1L,
                "会员支付流水表",
                "pay_trade",
                "payment",
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of("进入 DDL 预览"),
                "prompt");
        when(service.draft(req)).thenReturn(result);
        RequirementDraftController controller = new RequirementDraftController(service);

        var response = controller.draft(req);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isSameAs(result);
        verify(service).draft(req);
    }
}
